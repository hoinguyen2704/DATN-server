package com.hoz.hozitech.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 * When a customer clicks "Pay" and gets redirected to the VNPAY/MoMo payment page
 * but abandons the payment (closes tab, presses back), the order remains PENDING
 * while inventory is locked and coupon usage is consumed.
 * <p>
 * This scheduler runs every 5 minutes and cancels any PENDING online-payment order
 * older than 30 minutes, restoring inventory and coupon counts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StalePendingOrderCanceller {

    private static final int STALE_THRESHOLD_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderCheckoutHelper checkoutHelper;
    private final CouponApplier couponApplier;
    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;

    /**
     * Runs every 5 minutes. Finds PENDING orders with online payment methods
     * (excluding COD) that have been PENDING for more than 30 minutes.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void cancelStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);

        List<Order> staleOrders = orderRepository.findStalePendingOrders(
                OrderStatus.PENDING, PaymentMethod.COD, cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("[StalePendingOrderCanceller] Found {} stale PENDING orders to cancel", staleOrders.size());

        for (Order order : staleOrders) {
            try {
                cancelOrder(order);
                log.info("[StalePendingOrderCanceller] Auto-cancelled order {} (created: {}, payment: {})",
                        order.getOrderNumber(), order.getCreatedAt(), order.getPaymentMethod());
            } catch (Exception e) {
                log.error("[StalePendingOrderCanceller] Failed to cancel order {}: {}",
                        order.getOrderNumber(), e.getMessage(), e);
            }
        }
    }

    private void cancelOrder(Order order) {
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
                .description("Đơn hàng tự động hủy do chưa thanh toán sau " + STALE_THRESHOLD_MINUTES + " phút")
                .build());

        // Notify user
        notificationService.createForUser(order.getUser().getId(), UserNotificationTemplates.orderAutoCancelled(order));
        adminNotificationService.createShared(AdminNotificationTemplates.orderAutoCancelled(order), false);
    }
}
