package com.fitting.paymentservice.entity;

public enum PaymentStatus {
    PENDING,      // iniciado, esperando procesamiento
    COMPLETED,    // pago aprobado
    FAILED,       // pago rechazado
    REFUNDED      // reembolsado
}