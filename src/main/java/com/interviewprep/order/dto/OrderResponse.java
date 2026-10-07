package com.interviewprep.order.dto;

import com.interviewprep.order.CustomerOrder;
import com.interviewprep.order.OrderItem;
import com.interviewprep.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id, OrderStatus status, BigDecimal total, List<Line> items, Instant createdAt) {

  public record Line(
      Long productId,
      String productName,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal) {

    static Line from(OrderItem item) {
      return new Line(
          item.getProductId(),
          item.getProductName(),
          item.getUnitPrice(),
          item.getQuantity(),
          item.lineTotal());
    }
  }

  public static OrderResponse from(CustomerOrder order) {
    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getTotal(),
        order.getItems().stream().map(Line::from).toList(),
        order.getCreatedAt());
  }
}
