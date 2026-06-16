package com.example.ims.dto;

import com.example.ims.domain.Product;

public record ProductResponse(
        Long id,
        String name,
        int quantity
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getQuantity());
    }
}
