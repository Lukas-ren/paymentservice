package com.fitting.paymentservice.dto;

import com.fitting.paymentservice.entity.PaymentMethod;
import com.fitting.paymentservice.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del pago retornados por la API")
public class PaymentResponse {

    @Schema(description = "ID del pago", example = "1")
    private Long id;

    @Schema(description = "Código de transacción", example = "PAY-20260522-0001")
    private String transactionCode;

    @Schema(description = "ID de la orden", example = "1")
    private Long orderId;

    @Schema(description = "Número de orden", example = "ORD-20260522-0001")
    private String orderNumber;

    @Schema(description = "Nombre del cliente", example = "Juan Pérez")
    private String customerName;

    @Schema(description = "Email del cliente", example = "juan@fitting.com")
    private String customerEmail;

    @Schema(description = "Monto pagado", example = "89.97")
    private BigDecimal amount;

    @Schema(description = "Método de pago", example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Estado del pago", example = "COMPLETED",
            allowableValues = {"PENDING","COMPLETED","FAILED","REFUNDED"})
    private PaymentStatus status;

    @Schema(description = "Notas del pago", example = "Pago aprobado")
    private String notes;

    @Schema(description = "Fecha de creación", example = "2026-05-22T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de actualización", example = "2026-05-22T10:35:00")
    private LocalDateTime updatedAt;
}