package com.interviewprep.order;

import com.interviewprep.order.dto.OrderResponse;

public record PlacedOrder(OrderResponse order, boolean replayed) {}
