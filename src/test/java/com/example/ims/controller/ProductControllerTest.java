package com.example.ims.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ims.domain.Product;
import com.example.ims.repository.ProductRepository;
import com.example.ims.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    private final MockMvc mockMvc;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Autowired
    ProductControllerTest(
            MockMvc mockMvc,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.mockMvc = mockMvc;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void inboundCreatesProduct() throws Exception {
        mockMvc.perform(post("/api/products/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Product A",
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Product A"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void inboundRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/products/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    void stockChangeRejectsNonPositiveQuantity() throws Exception {
        Product product = productRepository.saveAndFlush(new Product("Product A", 10));

        mockMvc.perform(post("/api/products/{productId}/outbound", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("quantity")));
    }

    @Test
    void outboundReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/outbound", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void outboundRejectsInsufficientStock() throws Exception {
        Product product = productRepository.saveAndFlush(new Product("Product A", 1));

        mockMvc.perform(post("/api/products/{productId}/outbound", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void getStockReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/{productId}/stock", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void malformedJsonReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/products/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void invalidPathVariableReturnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
