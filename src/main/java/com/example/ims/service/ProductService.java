package com.example.ims.service;

import com.example.ims.domain.Product;
import com.example.ims.domain.StockMovement;
import com.example.ims.domain.StockMovementType;
import com.example.ims.dto.CreateInboundRequest;
import com.example.ims.dto.ProductResponse;
import com.example.ims.dto.StockResponse;
import com.example.ims.exception.BusinessException;
import com.example.ims.exception.ErrorCode;
import com.example.ims.repository.ProductRepository;
import com.example.ims.repository.StockMovementRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductService {

    private static final int PRODUCT_NAME_LOCK_STRIPES = 64;

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReentrantLock[] productNameLocks = createProductNameLocks();

    public ProductService(
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ProductResponse inbound(CreateInboundRequest request) {
        String productName = request.name().trim();
        ReentrantLock lock = productNameLock(productName);

        lock.lock();
        try {
            return transactionTemplate.execute(status -> inboundByName(productName, request.quantity()));
        } finally {
            lock.unlock();
        }
    }

    private ProductResponse inboundByName(String productName, int quantity) {
        Optional<Product> lockedProduct = productRepository.findByNameForUpdate(productName);
        Product product;
        if (lockedProduct.isPresent()) {
            product = lockedProduct.get();
            product.inbound(quantity);
        } else {
            product = productRepository.save(new Product(productName, quantity));
        }

        stockMovementRepository.save(new StockMovement(product, StockMovementType.INBOUND, quantity));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse inbound(Long productId, int quantity) {
        Product product = findProductForUpdate(productId);
        product.inbound(quantity);
        stockMovementRepository.save(new StockMovement(product, StockMovementType.INBOUND, quantity));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse outbound(Long productId, int quantity) {
        Product product = findProductForUpdate(productId);
        if (product.getQuantity() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        product.outbound(quantity);
        stockMovementRepository.save(new StockMovement(product, StockMovementType.OUTBOUND, quantity));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return StockResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    private Product findProductForUpdate(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ReentrantLock productNameLock(String productName) {
        int index = Math.floorMod(productName.hashCode(), productNameLocks.length);
        return productNameLocks[index];
    }

    private static ReentrantLock[] createProductNameLocks() {
        ReentrantLock[] locks = new ReentrantLock[PRODUCT_NAME_LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        return locks;
    }
}
