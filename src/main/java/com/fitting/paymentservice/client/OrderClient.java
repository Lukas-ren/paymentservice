package com.fitting.paymentservice.client;

import com.fitting.paymentservice.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "order-service", url = "${order.service.url}")
public interface OrderClient {

    // Obtiene datos de la orden (número, cliente, monto, estado)
    @GetMapping("/api/v1/orders/{id}")
    ApiResponse<Map<String, Object>> getOrderById(@PathVariable Long id);

    // Avanza la orden a CONFIRMED tras pago exitoso
    @PatchMapping("/api/v1/orders/{id}/status")
    ApiResponse<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status);

    // Cancela la orden si el pago falla (libera stock en cascada)
    @PatchMapping("/api/v1/orders/{id}/cancel")
    ApiResponse<Void> cancelOrder(@PathVariable Long id);
}