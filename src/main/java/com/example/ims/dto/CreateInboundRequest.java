package com.example.ims.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateInboundRequest(
        @NotBlank String name,
        @Min(1) int quantity
) {
}
