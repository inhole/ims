package com.example.ims.dto;

import com.example.ims.domain.Product;

public record StockResponse(
        Long productId,
        String name,
        int quantity
) {

    public static StockResponse from(Product product) {
        return new StockResponse(product.getId(), product.getName(), product.getQuantity());
    }
}
