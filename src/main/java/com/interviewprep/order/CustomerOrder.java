package com.interviewprep.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "orders",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_orders_customer_idempotency_key",
            columnNames = {"customer_email", "idempotency_key"}))
public class CustomerOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_email", nullable = false, length = 254)
  private String customerEmail;

  @Column(name = "idempotency_key", nullable = false, length = 100)
  private String idempotencyKey;

  @Column(nullable = false, length = 64)
  private String requestHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private OrderStatus status;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("productId ASC")
  private List<OrderItem> items = new ArrayList<>();

  protected CustomerOrder() {}

  public CustomerOrder(String customerEmail, String idempotencyKey, String requestHash) {
    this.customerEmail = customerEmail;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.status = OrderStatus.PLACED;
    this.total = BigDecimal.ZERO;
  }

  public void addItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
    OrderItem item = new OrderItem(this, productId, productName, unitPrice, quantity);
    items.add(item);
    total = total.add(item.lineTotal());
  }

  public Long getId() {
    return id;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<OrderItem> getItems() {
    return List.copyOf(items);
  }
}
