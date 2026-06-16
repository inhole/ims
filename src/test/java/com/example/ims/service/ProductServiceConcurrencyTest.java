package com.example.ims.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.example.ims.domain.Product;
import com.example.ims.dto.CreateInboundRequest;
import com.example.ims.exception.BusinessException;
import com.example.ims.exception.ErrorCode;
import com.example.ims.repository.ProductRepository;
import com.example.ims.repository.StockMovementRepository;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceConcurrencyTest {

    private static final int THREAD_COUNT = 20;

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Autowired
    ProductServiceConcurrencyTest(
            ProductService productService,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void concurrentInboundRequestsKeepStockConsistent() throws Exception {
        Product product = productRepository.saveAndFlush(new Product("Product A", 1));

        runConcurrently(THREAD_COUNT, () -> productService.inbound(product.getId(), 1));

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getQuantity()).isEqualTo(THREAD_COUNT + 1);
    }

    @Test
    void concurrentOutboundRequestsKeepStockConsistent() throws Exception {
        Product product = productRepository.saveAndFlush(new Product("Product A", THREAD_COUNT));

        runConcurrently(THREAD_COUNT, () -> productService.outbound(product.getId(), 1));

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getQuantity()).isZero();
    }

    @Test
    void concurrentOutboundRequestsNeverMakeStockNegative() throws Exception {
        Product product = productRepository.saveAndFlush(new Product("Product A", 10));
        AtomicInteger insufficientStockCount = new AtomicInteger();

        runConcurrently(THREAD_COUNT, () -> {
            try {
                productService.outbound(product.getId(), 1);
            } catch (BusinessException exception) {
                if (exception.getErrorCode() != ErrorCode.INSUFFICIENT_STOCK) {
                    throw exception;
                }
                insufficientStockCount.incrementAndGet();
            }
        });

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getQuantity()).isZero();
        assertThat(insufficientStockCount.get()).isEqualTo(10);
    }

    @Test
    void concurrentInboundRequestsForNewProductCreateOneProduct() throws Exception {
        runConcurrently(THREAD_COUNT, () -> productService.inbound(new CreateInboundRequest("Product A", 1)));

        Product found = productRepository.findByName("Product A").orElseThrow();
        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(found.getQuantity()).isEqualTo(THREAD_COUNT);
    }

    private static void runConcurrently(int taskCount, ThrowingRunnable task) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(taskCount);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < taskCount; i++) {
            executorService.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    task.run();
                } catch (Throwable throwable) {
                    failures.add(throwable);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        if (!failures.isEmpty()) {
            fail("Concurrent task failed", failures.peek());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
