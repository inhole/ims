package com.example.ims.controller;

import com.example.ims.dto.CreateInboundRequest;
import com.example.ims.dto.ProductResponse;
import com.example.ims.dto.StockChangeRequest;
import com.example.ims.dto.StockResponse;
import com.example.ims.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "상품 재고 관리 API")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이름 기준 입고", description = "상품이 없으면 신규 등록하고, 이미 있으면 현재 재고를 증가시킵니다.")
    public ProductResponse inbound(@Valid @RequestBody CreateInboundRequest request) {
        return productService.inbound(request);
    }

    @PostMapping("/{productId}/inbound")
    @Operation(summary = "기존 상품 입고", description = "상품 ID 기준으로 현재 재고를 증가시킵니다.")
    public ProductResponse inbound(
            @PathVariable Long productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return productService.inbound(productId, request.quantity());
    }

    @PostMapping("/{productId}/outbound")
    @Operation(summary = "기존 상품 출고", description = "상품 ID 기준으로 현재 재고를 감소시킵니다. 재고는 음수가 될 수 없습니다.")
    public ProductResponse outbound(
            @PathVariable Long productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return productService.outbound(productId, request.quantity());
    }

    @GetMapping("/{productId}/stock")
    @Operation(summary = "재고 조회", description = "상품 ID 기준으로 현재 재고 수량을 조회합니다.")
    public StockResponse getStock(@PathVariable Long productId) {
        return productService.getStock(productId);
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "등록된 상품 목록과 현재 재고를 조회합니다.")
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }
}
