package com.interviewprep.product.dto;

import java.math.BigDecimal;

public record ProductFilter(
    String category, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, String name) {}
