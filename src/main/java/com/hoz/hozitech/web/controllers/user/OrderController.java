package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.order.OrderService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestAPI("${api.prefix-client}/orders")
@Authenticated
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LocalizedApiResponseFactory responseFactory;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpServletRequest) {
        String ipAddress = getClientIp(httpServletRequest);
        OrderResponse response = orderService.checkout(userDetails.getUser().getId(), request, idempotencyKey, ipAddress);
        return ResponseEntity.ok(responseFactory.success("response.order.created", response));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(responseFactory.success(
                "response.order.fetched",
                orderService.getOrderByNumber(orderNumber, userDetails.getUser().getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success(
                "response.order.list_fetched",
                orderService.getMyOrders(userDetails.getUser().getId(), status, keyword, page, size)));
    }

    @PatchMapping("/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(responseFactory.success(
                "response.order.cancelled",
                orderService.cancelOrder(userDetails.getUser().getId(), orderNumber)));
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // In case of multiple IPs (e.g. "client-ip, proxy1, proxy2"), get the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
