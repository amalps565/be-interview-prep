package com.interviewprep.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrder> findByCustomerEmailAndIdempotencyKey(
      String customerEmail, String idempotencyKey);

  @EntityGraph(attributePaths = "items")
  Optional<CustomerOrder> findByIdAndCustomerEmail(Long id, String customerEmail);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update CustomerOrder o set o.status = com.interviewprep.order.OrderStatus.CANCELLED"
          + " where o.id = :id and o.status = com.interviewprep.order.OrderStatus.PLACED")
  int cancelIfPlaced(@Param("id") Long id);
}
