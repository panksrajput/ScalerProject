package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    Page<Order> findByUserIdAndStatus(
        @Param("userId") Long userId, 
        @Param("status") Order.OrderStatus status, 
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.updatedAt < :threshold")
    List<Order> findOldOrdersByStatus(
        @Param("status") Order.OrderStatus status,
        @Param("threshold") LocalDateTime threshold
    );
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.paymentStatus = :paymentStatus")
    Page<Order> findByUserIdAndPaymentStatus(
        @Param("userId") Long userId, 
        @Param("paymentStatus") Order.PaymentStatus paymentStatus, 
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o WHERE o.updatedAt BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderNumber LIKE %:query%")
    Page<Order> searchByOrderNumber(
        @Param("userId") Long userId,
        @Param("query") String query,
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    Page<Order> findByStatus(
        @Param("status") Order.OrderStatus status,
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :paymentStatus")
    Page<Order> findByPaymentStatus(
        @Param("paymentStatus") Order.PaymentStatus paymentStatus,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.updatedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSalesBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    List<Order> findByLockedTrueAndLockedAtBefore(LocalDateTime time);

    Optional<Order> findById(Long id);
}
