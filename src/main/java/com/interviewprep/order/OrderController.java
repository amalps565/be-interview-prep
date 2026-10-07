package com.interviewprep.order;

import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.order.dto.OrderResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  static final String REPLAYED_HEADER = "Idempotent-Replayed";

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> place(
      @RequestHeader(OrderService.IDEMPOTENCY_KEY) String idempotencyKey,
      @Valid @RequestBody OrderRequest request,
      Authentication authentication) {
    PlacedOrder placed = orderService.place(authentication.getName(), idempotencyKey, request);
    OrderResponse order = placed.order();
    if (placed.replayed()) {
      return ResponseEntity.ok().header(REPLAYED_HEADER, "true").body(order);
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .location(URI.create("/api/orders/" + order.id()))
        .body(order);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable Long id, Authentication authentication) {
    return orderService.get(authentication.getName(), id);
  }

  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
    return orderService.cancel(authentication.getName(), id);
  }
}
