package com.fitting.paymentservice.service.impl;

import com.fitting.paymentservice.client.OrderClient;
import com.fitting.paymentservice.dto.PaymentRequest;
import com.fitting.paymentservice.dto.PaymentResponse;
import com.fitting.paymentservice.entity.Payment;
import com.fitting.paymentservice.entity.PaymentStatus;
import com.fitting.paymentservice.exception.BusinessException;
import com.fitting.paymentservice.exception.ResourceNotFoundException;
import com.fitting.paymentservice.repository.PaymentRepository;
import com.fitting.paymentservice.service.PaymentService;
import com.fitting.paymentservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;

    private static final AtomicLong sequence = new AtomicLong(1);

    // ── Procesar pago ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Procesando pago para orden ID: {}", request.getOrderId());

        // 1. Verificar que no exista un pago previo completado para esta orden
        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                throw new BusinessException(
                        "La orden ID " + request.getOrderId() + " ya tiene un pago completado");
            }
        });

        // 2. Obtener datos de la orden desde order-service
        Map<String, Object> orderData = fetchOrder(request.getOrderId());

        String orderNumber   = (String) orderData.get("orderNumber");
        String customerName  = (String) orderData.get("customerName");
        String customerEmail = (String) orderData.get("customerEmail");
        String orderStatus   = (String) orderData.get("status");
        double orderTotal    = Double.parseDouble(orderData.get("totalAmount").toString());

        // 3. Validaciones de negocio
        if (!"PENDING".equals(orderStatus)) {
            throw new BusinessException(
                    "Solo se pueden pagar órdenes en estado PENDING. Estado actual: " + orderStatus);
        }

        if (request.getAmount().doubleValue() != orderTotal) {
            throw new BusinessException(String.format(
                    "El monto del pago (%.2f) no coincide con el total de la orden (%.2f)",
                    request.getAmount().doubleValue(), orderTotal));
        }

        // 4. Simular procesamiento del pago (en producción iría el gateway real)
        PaymentStatus resultStatus = simulatePaymentGateway(request);

        // 5. Registrar el pago
        Payment payment = Payment.builder()
                .transactionCode(generateTransactionCode())
                .orderId(request.getOrderId())
                .orderNumber(orderNumber)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(resultStatus)
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);

        // 6. Notificar a order-service según resultado
        if (resultStatus == PaymentStatus.COMPLETED) {
            confirmOrder(request.getOrderId(), orderNumber);
            log.info("Pago completado. Transacción: {} — Orden: {}", saved.getTransactionCode(), orderNumber);
        } else {
            cancelOrderOnFailure(request.getOrderId(), orderNumber);
            log.warn("Pago fallido. Transacción: {} — Orden cancelada: {}", saved.getTransactionCode(), orderNumber);
        }

        return toResponse(saved);
    }

    // ── Consultas ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        log.debug("Buscando pago con ID: {}", id);
        return toResponse(getPaymentOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrderId(Long orderId) {
        log.debug("Buscando pago para orden ID: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pago para orden ID " + orderId + " no encontrado"));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByTransactionCode(String transactionCode) {
        log.debug("Buscando pago por transacción: {}", transactionCode);
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transacción " + transactionCode + " no encontrada"));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        log.debug("Listando todos los pagos");
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findByCustomerEmail(String email) {
        log.debug("Buscando pagos del cliente: {}", email);
        return paymentRepository.findByCustomerEmail(email).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findByStatus(PaymentStatus status) {
        log.debug("Buscando pagos con estado: {}", status);
        return paymentRepository.findByStatus(status).stream()
                .map(this::toResponse).toList();
    }

    // ── Reembolso ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse refund(Long id) {
        log.info("Procesando reembolso para pago ID: {}", id);

        Payment payment = getPaymentOrThrow(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(
                    "Solo se pueden reembolsar pagos completados. Estado actual: "
                            + payment.getStatus());
        }

        // Cancelar la orden asociada (libera stock en cascada)
        cancelOrderOnFailure(payment.getOrderId(), payment.getOrderNumber());

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setNotes("Reembolso procesado");

        Payment updated = paymentRepository.save(payment);
        log.info("Reembolso completado. Transacción: {}", updated.getTransactionCode());
        return toResponse(updated);
    }

    // ── Helpers Feign ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchOrder(Long orderId) {
        try {
            ApiResponse<Map<String, Object>> response = orderClient.getOrderById(orderId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("Orden ID " + orderId + " no encontrada");
            }
            return response.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error consultando order-service para orden {}: {}", orderId, ex.getMessage());
            throw new BusinessException("No se pudo obtener información de la orden ID: " + orderId);
        }
    }

    private void confirmOrder(Long orderId, String orderNumber) {
        try {
            orderClient.updateOrderStatus(orderId, "CONFIRMED");
            log.debug("Orden {} confirmada en order-service", orderNumber);
        } catch (Exception ex) {
            // Logeamos pero no revertimos el pago; se puede reintentar manualmente
            log.error("Error confirmando orden {} en order-service: {}", orderNumber, ex.getMessage());
        }
    }

    private void cancelOrderOnFailure(Long orderId, String orderNumber) {
        try {
            orderClient.cancelOrder(orderId);
            log.debug("Orden {} cancelada en order-service", orderNumber);
        } catch (Exception ex) {
            log.error("Error cancelando orden {} en order-service: {}", orderNumber, ex.getMessage());
        }
    }

    // ── Simulador de gateway ─────────────────────────────────────────────────────
    // En producción aquí iría la integración real (Stripe, MercadoPago, etc.)

    private PaymentStatus simulatePaymentGateway(PaymentRequest request) {
        return switch (request.getPaymentMethod()) {
            case CASH_ON_DELIVERY -> PaymentStatus.PENDING;   // se confirma al entregar
            default               -> PaymentStatus.COMPLETED; // el resto se aprueba
        };
    }

    // ── Generador de código de transacción ──────────────────────────────────────

    private String generateTransactionCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq  = String.format("%04d", sequence.getAndIncrement());
        String candidate = "PAY-" + date + "-" + seq;

        while (paymentRepository.findByTransactionCode(candidate).isPresent()) {
            candidate = "PAY-" + date + "-" + String.format("%04d", sequence.getAndIncrement());
        }
        return candidate;
    }

    // ── Helper repository ───────────────────────────────────────────────────────

    private Payment getPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
    }

    // ── Mapper interno ──────────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .transactionCode(p.getTransactionCode())
                .orderId(p.getOrderId())
                .orderNumber(p.getOrderNumber())
                .customerName(p.getCustomerName())
                .customerEmail(p.getCustomerEmail())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}