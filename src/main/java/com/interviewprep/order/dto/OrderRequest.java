package com.interviewprep.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
    @NotEmpty(message = "must contain at least one item")
        @Size(max = 50, message = "must contain at most 50 items")
        List<@Valid OrderItemRequest> items) {}
