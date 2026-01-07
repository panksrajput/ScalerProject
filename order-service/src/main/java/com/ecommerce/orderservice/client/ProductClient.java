package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductDto;
import com.ecommerce.orderservice.dto.request.StockReduceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;

@FeignClient(name = "product-service", url = "${product.service.url}")
public interface ProductClient {
    
    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long productId);

    @PostMapping("/api/products/decrement-stock/batch")
    void reduceStockBatch(@RequestBody Set<StockReduceRequest> requests);
}
