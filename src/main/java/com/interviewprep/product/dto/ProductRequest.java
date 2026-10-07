package com.interviewprep.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters")
        String name,
    @NotBlank(message = "is required") @Size(max = 50, message = "must be at most 50 characters")
        String category,
    @NotNull(message = "is required")
        @DecimalMin(value = "0.00", message = "must not be negative")
        @Digits(integer = 8, fraction = 2, message = "must have at most 8 digits and 2 decimals")
        BigDecimal price,
    @NotNull(message = "is required") @Min(value = 0, message = "must not be negative")
        Integer stock,
    @NotNull(message = "is required")
        @DecimalMin(value = "0.0", message = "must be between 0 and 5")
        @DecimalMax(value = "5.0", message = "must be between 0 and 5")
        Double rating) {}
