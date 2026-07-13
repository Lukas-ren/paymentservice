package com.fitting.paymentservice.client;

import com.fitting.paymentservice.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/v1/orders/{id}")
    ApiResponse<Map<String, Object>> getOrderById(@PathVariable Long id);

    @PatchMapping("/api/v1/orders/{id}/status")
    ApiResponse<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status);

    @PatchMapping("/api/v1/orders/{id}/cancel")
    ApiResponse<Void> cancelOrder(@PathVariable Long id);
}