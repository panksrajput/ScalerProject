package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.client.OrderClient;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.PaymentResultEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentEventProducer producer;
    private final OrderClient orderClient;

    @Value("${payu.key}")
    private String key;

    @Value("${payu.base-url}")
    private String payuUrl;

    @Value("${app.url}")
    private String appUrl;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/create")
    public Map<String, String> createPayment(@RequestBody Map<String, Object> req) {

        Double amount = Double.valueOf(req.get("amount").toString());
        String email = req.get("email").toString();
        String firstname = req.get("firstname").toString();
        String orderId = req.get("orderId").toString();

        Payment tx = paymentService.createTransaction(amount, email, firstname);
        String hash = paymentService.generateHash(tx, orderId, tx.getId().toString());

        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("txnId", tx.getTxnId());
        response.put("amount", tx.getAmount().toString());
        response.put("productInfo", tx.getProductInfo());
        response.put("firstname", tx.getFirstname());
        response.put("email", tx.getEmail());
        response.put("hash", hash);
        response.put("payuUrl", payuUrl);
        response.put("paymentId", tx.getId().toString());

        response.put("surl", appUrl + "/api/payment/success");
        response.put("furl", appUrl + "/api/payment/failure");

        return response;
    }

    @PostMapping("/success")
    public void success(
            @RequestParam Map<String, String> params,
            HttpServletResponse response
    ) throws IOException {
        String txnId = params.get("txnid");
        logger.debug("Payment transaction was successful with id: {}", txnId);
        paymentService.updateStatus(txnId, "SUCCESS", params.toString());

        producer.publish(
                PaymentResultEvent.builder()
                        .orderId(Long.parseLong(params.get("udf1")))
                        .paymentId(Long.parseLong(params.get("udf2")))
                        .transactionId(params.get("txnid"))
                        .success(true)
                        .build()
        );

        // Redirect browser to frontend (GET)
        response.sendRedirect(
                frontendUrl + "/payment-success.html?txnid=" + txnId
        );
    }

    @PostMapping("/failure")
    public void failure(
            @RequestParam Map<String, String> params,
            HttpServletResponse response
    ) throws IOException {

        String txnId = params.get("txnid");
        paymentService.updateStatus(txnId, "FAILED", params.toString());

        producer.publish(
                PaymentResultEvent.builder()
                        .orderId(Long.parseLong(params.get("udf1")))
                        .paymentId(Long.parseLong(params.get("udf2")))
                        .transactionId(params.get("txnid"))
                        .success(false)
                        .build()
        );

        response.sendRedirect(
                frontendUrl + "/payment-failure.html?txnid=" + txnId
        );
    }

}
