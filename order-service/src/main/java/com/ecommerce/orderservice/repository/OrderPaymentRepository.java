package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OrderPaymentRepository
        extends JpaRepository<OrderPayment, Long> {

    Optional<OrderPayment> findByPaymentId(Long paymentId);

    @Modifying
    @Transactional
    @Query("UPDATE OrderPayment op SET op.status = :status, op.updatedAt = CURRENT_TIMESTAMP WHERE op.paymentId = :paymentId")
    int updateStatus(
            @Param("paymentId") Long paymentId,
            @Param("status") String status
    );
}
