package com.ecommerce.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for product %s. Available: %d, Requested: %d", 
            productName, available, requested));
    }
}
