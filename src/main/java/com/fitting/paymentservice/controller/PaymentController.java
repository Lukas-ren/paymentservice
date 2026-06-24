package com.fitting.paymentservice.controller;

import com.fitting.paymentservice.dto.PaymentRequest;
import com.fitting.paymentservice.dto.PaymentResponse;
import com.fitting.paymentservice.entity.PaymentStatus;
import com.fitting.paymentservice.service.PaymentService;
import com.fitting.paymentservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Pagos", description = "Procesamiento de pagos y reembolsos")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Procesar pago", description = "Procesa el pago y confirma o cancela la orden automáticamente")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pago procesado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Monto incorrecto o la orden no está en PENDING")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        log.info("POST /api/v1/payments — orden ID: {}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pago procesado",
                        paymentService.processPayment(request)));
    }

    @Operation(summary = "Listar pagos")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> findAll() {
        log.info("GET /api/v1/payments");
        return ResponseEntity.ok(ApiResponse.ok("Lista de pagos",
                paymentService.findAll()));
    }

    @Operation(summary = "Buscar pago por ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> findById(@PathVariable Long id) {
        log.info("GET /api/v1/payments/{}", id);
        return ResponseEntity.ok(ApiResponse.ok("Pago encontrado",
                paymentService.findById(id)));
    }

    @Operation(summary = "Buscar por código de transacción")
    @GetMapping("/transaction/{code}")
    public ResponseEntity<ApiResponse<PaymentResponse>> findByTransactionCode(
            @PathVariable String code) {
        log.info("GET /api/v1/payments/transaction/{}", code);
        return ResponseEntity.ok(ApiResponse.ok("Pago encontrado",
                paymentService.findByTransactionCode(code)));
    }

    @Operation(summary = "Buscar pago por orden")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> findByOrderId(
            @PathVariable Long orderId) {
        log.info("GET /api/v1/payments/order/{}", orderId);
        return ResponseEntity.ok(ApiResponse.ok("Pago de la orden",
                paymentService.findByOrderId(orderId)));
    }

    @Operation(summary = "Pagos por cliente")
    @GetMapping("/customer/{email}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> findByCustomer(
            @PathVariable String email) {
        log.info("GET /api/v1/payments/customer/{}", email);
        return ResponseEntity.ok(ApiResponse.ok("Pagos del cliente",
                paymentService.findByCustomerEmail(email)));
    }

    @Operation(summary = "Filtrar pagos por estado")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> findByStatus(
            @PathVariable PaymentStatus status) {
        log.info("GET /api/v1/payments/status/{}", status);
        return ResponseEntity.ok(ApiResponse.ok("Pagos por estado",
                paymentService.findByStatus(status)));
    }

    @Operation(summary = "Reembolsar pago", description = "Reembolsa el pago y cancela la orden asociada")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reembolso procesado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Solo se pueden reembolsar pagos COMPLETED")
    })
    @PatchMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(@PathVariable Long id) {
        log.info("PATCH /api/v1/payments/{}/refund", id);
        return ResponseEntity.ok(ApiResponse.ok("Reembolso procesado",
                paymentService.refund(id)));
    }
}