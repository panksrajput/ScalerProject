package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${product.service.url}")
public interface ProductClient {
    
    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long productId);
    
    @GetMapping("/api/products/{id}/exists")
    boolean productExists(@PathVariable("id") Long productId);
}
