package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", url = "${cart.service.url}")
public interface CartClient {

    @GetMapping("/api/cart")
    CartDto getCart();

    @DeleteMapping("/api/cart")
    void clearCart();
}