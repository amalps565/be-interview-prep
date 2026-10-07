package com.interviewprep.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
    @NotNull(message = "is required") Long productId,
    @NotNull(message = "is required")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 1000, message = "must be at most 1000")
        Integer quantity) {}
