package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface OrderService {

    // User
    OrderResponse checkout(UUID userId, CheckoutRequest request);

    OrderResponse getOrderByNumber(String orderNumber, UUID userId);

    PageResponse<OrderResponse> getMyOrders(UUID userId, String status, int page, int size);

    OrderResponse cancelOrder(UUID userId, UUID orderId);

    // Admin
    PageResponse<OrderResponse> getAllOrders(String status, String keyword, int page, int size);

    OrderResponse updateOrderStatus(UUID orderId, String status);
}
