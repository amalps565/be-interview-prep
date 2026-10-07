package com.interviewprep.order;

import com.interviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

  public InsufficientStockException(String productName, Long productId, int requested) {
    super(
        HttpStatus.CONFLICT,
        "Insufficient stock for product "
            + productId
            + " ("
            + productName
            + "): requested "
            + requested
            + ", no items were reserved");
  }
}
