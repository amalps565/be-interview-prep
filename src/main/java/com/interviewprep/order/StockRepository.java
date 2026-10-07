package com.interviewprep.order;

import com.interviewprep.product.Product;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends Repository<Product, Long> {

  @Modifying
  @Query(
      "update Product p set p.stock = p.stock - :quantity"
          + " where p.id = :productId and p.stock >= :quantity")
  int reserve(@Param("productId") Long productId, @Param("quantity") int quantity);

  @Modifying
  @Query("update Product p set p.stock = p.stock + :quantity where p.id = :productId")
  int release(@Param("productId") Long productId, @Param("quantity") int quantity);
}
