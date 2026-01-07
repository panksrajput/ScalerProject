package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository repository;

    @Value("${payu.key}")
    private String key;

    @Value("${payu.salt}")
    private String salt;

    public Payment createTransaction(Double amount, String email, String firstname) {
        Payment tx = Payment.builder()
                .txnId(UUID.randomUUID().toString())
                .amount(amount)
                .productInfo("Cart Order")
                .email(email)
                .firstname(firstname)
                .status("CREATED")
                .build();
        return repository.save(tx);
    }

    public String generateHash(Payment tx, String orderId, String paymentId) {
        String hashString =
                key + "|" +
                        tx.getTxnId() + "|" +
                        tx.getAmount() + "|" +
                        tx.getProductInfo() + "|" +
                        tx.getFirstname() + "|" +
                        tx.getEmail() + "|" +
                        orderId + "|" +          // udf1
                        paymentId + "|" +        // udf2
                        "|" +                    // udf3
                        "|" +                    // udf4
                        "|" +                    // udf5
                        "|||||" +                // remaining empty fields
                        salt;

        return DigestUtils.sha512Hex(hashString);
    }

    public void updateStatus(String txnId, String status, String response) {
        logger.debug("Updating status of {} to {}" , txnId, status);
        Payment tx = repository.findByTxnId(txnId).orElseThrow();
        tx.setStatus(status);
        tx.setPayuResponse(response);
        repository.save(tx);
    }
}
