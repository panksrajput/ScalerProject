package com.ecommerce.cartservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CartServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CartServiceException(String message) {
        super(message);
    }
}
