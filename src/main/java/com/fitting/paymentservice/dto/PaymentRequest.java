package com.fitting.paymentservice.dto;

import com.fitting.paymentservice.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para procesar un pago")
public class PaymentRequest {

    @Schema(description = "ID de la orden a pagar", example = "1")
    @NotNull(message = "El ID de la orden es obligatorio")
    private Long orderId;

    @Schema(description = "Método de pago", example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD","DEBIT_CARD","BANK_TRANSFER","CASH_ON_DELIVERY"})
    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod paymentMethod;

    @Schema(description = "Monto a pagar", example = "89.97")
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Schema(description = "Notas adicionales", example = "Pago con tarjeta Visa")
    @Size(max = 255)
    private String notes;
}