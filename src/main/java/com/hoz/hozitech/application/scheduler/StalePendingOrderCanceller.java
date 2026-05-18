package com.hoz.hozitech.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.config.payment.BankTransferProperties;
import com.hoz.hozitech.application.config.payment.MomoProperties;
import com.hoz.hozitech.application.config.payment.VnpayProperties;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.notification.UserNotificationTemplates;
import com.hoz.hozitech.application.services.order.OrderCheckoutHelper;
import com.hoz.hozitech.application.services.order.CouponApplier;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job that auto-cancels "zombie" PENDING orders.
 * <p>
 * When a customer clicks "Pay" and abandons the online payment, or chooses
 * bank transfer but never completes it, the order remains PENDING while
 * inventory is locked and coupon usage is consumed.
 * <p>
 * This scheduler runs on the configured interval and cancels any unpaid PENDING
 * order older than that payment method's configured timeout, restoring inventory
 * and coupon counts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StalePendingOrderCanceller {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderCheckoutHelper checkoutHelper;
    private final CouponApplier couponApplier;
    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;
    private final VnpayProperties vnpayProperties;
    private final MomoProperties momoProperties;
    private final BankTransferProperties bankTransferProperties;

    /**
     * Finds unpaid PENDING VNPAY/MoMo/bank-transfer orders beyond the configured timeout.
     */
    @Scheduled(fixedRateString = "${payment.pending-order.scan-rate-ms:300000}")
    @Transactional
    public void cancelStaleOrders() {
        cancelStaleOrdersForMethod(PaymentMethod.VNPAY, vnpayProperties.getPendingTimeoutMinutes());
        cancelStaleOrdersForMethod(PaymentMethod.MOMO, momoProperties.getPendingTimeoutMinutes());
        cancelStaleOrdersForMethod(PaymentMethod.BANK_TRANSFER, bankTransferProperties.getPendingTimeoutMinutes());
    }

    private void cancelStaleOrdersForMethod(PaymentMethod paymentMethod, int timeoutMinutes) {
        int safeTimeoutMinutes = Math.max(1, timeoutMinutes);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(safeTimeoutMinutes);

        List<Order> staleOrders = orderRepository.findStalePendingOrders(
                OrderStatus.PENDING, PaymentStatus.PENDING, List.of(paymentMethod), cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("[StalePendingOrderCanceller] Found {} stale PENDING {} orders older than {} minutes to cancel",
                staleOrders.size(), paymentMethod, safeTimeoutMinutes);

        for (Order order : staleOrders) {
            try {
                cancelOrder(order, safeTimeoutMinutes);
                log.info("[StalePendingOrderCanceller] Auto-cancelled order {} (created: {}, payment: {})",
                        order.getOrderNumber(), order.getCreatedAt(), order.getPaymentMethod());
            } catch (Exception e) {
                log.error("[StalePendingOrderCanceller] Failed to cancel order {}: {}",
                        order.getOrderNumber(), e.getMessage(), e);
            }
        }
    }

    private void cancelOrder(Order order, int timeoutMinutes) {
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);

        // Restore inventory
        checkoutHelper.restoreStock(order);

        // Restore coupon usage
        couponApplier.restoreCouponUsage(order.getCouponCode(), order.getShippingCouponCode());

        orderRepository.save(order);

        // Log history
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .description("Đơn hàng tự động hủy do chưa thanh toán sau " + timeoutMinutes + " phút")
                .build());

        // Notify user
        notificationService.createForUser(order.getUser().getId(), UserNotificationTemplates.orderAutoCancelled(order));
        adminNotificationService.createShared(AdminNotificationTemplates.orderAutoCancelled(order), false);
    }
}
