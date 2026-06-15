package com.fitting.paymentservice.dto;

import com.fitting.paymentservice.entity.PaymentMethod;
import com.fitting.paymentservice.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private String transactionCode;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}