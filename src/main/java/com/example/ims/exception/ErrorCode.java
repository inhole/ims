package com.example.ims.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock quantity."),
    DUPLICATE_PRODUCT(HttpStatus.CONFLICT, "Product already exists."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
