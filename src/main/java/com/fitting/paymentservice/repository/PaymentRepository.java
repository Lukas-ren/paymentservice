package com.fitting.paymentservice.repository;

import com.fitting.paymentservice.entity.Payment;
import com.fitting.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByCustomerEmail(String customerEmail);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderId(Long orderId);
}