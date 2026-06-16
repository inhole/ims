package com.example.ims.controller;

import com.example.ims.dto.CreateInboundRequest;
import com.example.ims.dto.ProductResponse;
import com.example.ims.dto.StockChangeRequest;
import com.example.ims.dto.StockResponse;
import com.example.ims.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse inbound(@Valid @RequestBody CreateInboundRequest request) {
        return productService.inbound(request);
    }

    @PostMapping("/{productId}/inbound")
    public ProductResponse inbound(
            @PathVariable Long productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return productService.inbound(productId, request.quantity());
    }

    @PostMapping("/{productId}/outbound")
    public ProductResponse outbound(
            @PathVariable Long productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return productService.outbound(productId, request.quantity());
    }

    @GetMapping("/{productId}/stock")
    public StockResponse getStock(@PathVariable Long productId) {
        return productService.getStock(productId);
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }
}
