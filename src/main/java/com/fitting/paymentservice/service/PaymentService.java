package com.fitting.paymentservice.service;

import com.fitting.paymentservice.dto.PaymentRequest;
import com.fitting.paymentservice.dto.PaymentResponse;
import com.fitting.paymentservice.entity.PaymentStatus;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse findById(Long id);

    PaymentResponse findByOrderId(Long orderId);

    PaymentResponse findByTransactionCode(String transactionCode);

    List<PaymentResponse> findAll();

    List<PaymentResponse> findByCustomerEmail(String email);

    List<PaymentResponse> findByStatus(PaymentStatus status);

    PaymentResponse refund(Long id);
}