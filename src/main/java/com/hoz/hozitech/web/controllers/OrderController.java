package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.OrderService;
import com.hoz.hozitech.config.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-client}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Order fetched",
                orderService.getOrderByNumber(orderNumber, userDetails.getUser().getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Orders fetched",
                orderService.getMyOrders(userDetails.getUser().getId(), status, page, size)));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                orderService.cancelOrder(userDetails.getUser().getId(), orderId)));
    }
}
