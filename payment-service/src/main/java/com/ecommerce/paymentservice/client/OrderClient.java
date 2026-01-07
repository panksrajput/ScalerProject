package com.ecommerce.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${order.service.url}")
public interface OrderClient {
    @GetMapping("/api/orders/{orderNumber}/exists")
    boolean orderExists(@PathVariable("orderNumber") String orderNumber);
    
    @GetMapping("/api/orders/{orderNumber}/amount")
    Double getOrderAmount(@PathVariable("orderNumber") String orderNumber);

    @PutMapping("/api/order/payment/status")
    void updatePaymentStatus(@RequestParam String txnId,
                             @RequestParam String status);
}
