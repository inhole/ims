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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(ProductRepository productRepository, StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional
    public ProductResponse inbound(CreateInboundRequest request) {
        String productName = request.name().trim();
        Optional<Product> lockedProduct = productRepository.findByNameForUpdate(productName);
        Product product;
        if (lockedProduct.isPresent()) {
            product = lockedProduct.get();
            product.inbound(request.quantity());
        } else {
            product = productRepository.save(new Product(productName, request.quantity()));
        }

        stockMovementRepository.save(new StockMovement(product, StockMovementType.INBOUND, request.quantity()));
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
}
