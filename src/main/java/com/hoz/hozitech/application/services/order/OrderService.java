package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import com.hoz.hozitech.domain.dtos.response.AdminOrderListItemResponse;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface OrderService {

    OrderResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey, String ipAddress);

    OrderResponse getOrderByNumber(String orderNumber, UUID userId);

    OrderResponse getOrderByNumberForAdmin(String orderNumber);

    PageResponse<OrderResponse> getMyOrders(UUID userId, String status, String keyword, int page, int size);

    OrderResponse cancelOrder(UUID userId, String orderNumber);

    // Admin
    PageResponse<AdminOrderListItemResponse> getAllOrders(String status, String keyword, int page, int size, String sortBy, String sortDir);

    OrderResponse updateOrderStatus(UUID orderId, String status);

    // Public payment webhook
    OrderResponse handlePaymentWebhook(PaymentWebhookRequest request, String idempotencyKey);
}
