package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.MONEY_SCALE;
import static com.hoz.hozitech.application.services.order.OrderUtils.normalizeIdempotencyKey;
import static com.hoz.hozitech.application.services.order.OrderUtils.nz;
import static com.hoz.hozitech.application.services.order.OrderUtils.trimToNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.repositories.PaymentWebhookEventRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.notification.UserNotificationTemplates;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.entities.PaymentWebhookEvent;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;


// Handles payment webhook processing: idempotency, status transitions,
// monetary validation, and side effects (cancel, refund).

@Component
@RequiredArgsConstructor
class PaymentWebhookHandler {

    private final OrderRepository orderRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;
    private final OrderEmailSender orderEmailSender;
    private final OrderResponseMapper responseMapper;
    private final OrderCheckoutHelper checkoutHelper;
    private final CouponApplier couponApplier;
    private final SettingService settingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    OrderResponse handle(PaymentWebhookRequest request, String idempotencyKey) {
        String resolvedKey = resolveIdempotencyKey(request, idempotencyKey);
        acquireLock("payment-webhook:" + resolvedKey);

        PaymentWebhookEvent existingEvent = paymentWebhookEventRepository.findByIdempotencyKey(resolvedKey)
                .orElse(null);
        if (existingEvent != null) {
            if (existingEvent.getOrder() != null) {
                return responseMapper.mapToResponse(existingEvent.getOrder());
            }
            Order existingOrder = orderRepository.findByOrderNumber(existingEvent.getOrderNumber())
                    .orElseThrow(() -> new BusinessException(
                            BusinessErrorCode.WEBHOOK_ORDER_NOT_FOUND,
                            "Order not found for webhook event: " + existingEvent.getOrderNumber()));
            return responseMapper.mapToResponse(existingOrder);
        }

        PaymentStatus incomingStatus = parsePaymentStatus(request.getPaymentStatus());
        Order order = orderRepository.findByOrderNumberForUpdate(request.getOrderNumber())
                .orElseThrow(() -> new BusinessException(
                        BusinessErrorCode.WEBHOOK_ORDER_NOT_FOUND,
                        "Order not found: " + request.getOrderNumber()));
        validateMonetaryData(order, request, incomingStatus);

        applyTransition(order, incomingStatus);
        Order savedOrder = orderRepository.save(order);

        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .idempotencyKey(resolvedKey)
                .provider(normalizeProvider(request.getProvider()))
                .eventId(trimToNull(request.getEventId()))
                .orderNumber(savedOrder.getOrderNumber())
                .transactionId(trimToNull(request.getTransactionId()))
                .paymentStatus(incomingStatus)
                .responseCode(trimToNull(request.getResponseCode()))
                .rawPayload(trimToNull(request.getRawPayload()))
                .order(savedOrder)
                .build();
        paymentWebhookEventRepository.save(event);

        return responseMapper.mapToResponse(savedOrder);
    }

    private String resolveIdempotencyKey(PaymentWebhookRequest request, String webhookIdHeader) {
        String headerKey = normalizeIdempotencyKey(webhookIdHeader);
        if (headerKey != null) {
            return "wh:" + headerKey;
        }

        String eventId = normalizeIdempotencyKey(request.getEventId());
        if (eventId != null) {
            return "event:" + normalizeProvider(request.getProvider()) + ":" + eventId;
        }

        String transactionId = trimToNull(request.getTransactionId());
        if (transactionId != null) {
            String combined = normalizeProvider(request.getProvider())
                    + ":" + request.getOrderNumber()
                    + ":" + transactionId
                    + ":" + request.getPaymentStatus();
            return normalizeIdempotencyKey("tx:" + combined);
        }

        throw new BusinessException(
                BusinessErrorCode.WEBHOOK_IDEMPOTENCY_KEY_REQUIRED,
                "Webhook idempotency key is required (X-Webhook-Id, eventId, or transactionId)");
    }

    private void applyTransition(Order order, PaymentStatus incomingStatus) {
        PaymentStatus currentStatus = order.getPaymentStatus();

        if (currentStatus == incomingStatus) return;
        // Do not downgrade completed payments unless explicit REFUNDED.
        if (currentStatus == PaymentStatus.COMPLETED && incomingStatus != PaymentStatus.REFUNDED) return;
        // Once refunded, keep terminal state.
        if (currentStatus == PaymentStatus.REFUNDED) return;
        // Prevent re-opening a failed payment as completed without manual operation.
        if (currentStatus == PaymentStatus.FAILED && incomingStatus == PaymentStatus.COMPLETED) return;

        order.setPaymentStatus(incomingStatus);

        if (incomingStatus == PaymentStatus.FAILED) {
            if (order.getOrderStatus() != OrderStatus.CANCELLED
                    && order.getOrderStatus() != OrderStatus.SHIPPED
                    && order.getOrderStatus() != OrderStatus.RETURNED) {
                rollbackOrderAllocations(order);
                order.setOrderStatus(OrderStatus.CANCELLED);
                appendStatusHistory(order, OrderStatus.CANCELLED, "Thanh toán thất bại từ webhook, đơn đã huỷ");
            }
            notificationService.createForUser(order.getUser().getId(), UserNotificationTemplates.paymentFailed(order));
            adminNotificationService.createShared(AdminNotificationTemplates.paymentFailed(order), false);
            return;
        }

        if (incomingStatus == PaymentStatus.REFUNDED) {
            if (order.getOrderStatus() != OrderStatus.RETURNED) {
                order.setOrderStatus(OrderStatus.RETURNED);
                appendStatusHistory(order, OrderStatus.RETURNED, "Đơn hàng đã được hoàn tiền qua webhook");
            }
            notificationService.createForUser(order.getUser().getId(), UserNotificationTemplates.paymentRefunded(order));
            adminNotificationService.createShared(AdminNotificationTemplates.paymentRefunded(order), false);
            orderEmailSender.sendPaymentRefundedEmail(order);
            return;
        }

        if (incomingStatus == PaymentStatus.COMPLETED) {
            notificationService.createForUser(order.getUser().getId(), UserNotificationTemplates.paymentSuccess(order));
            adminNotificationService.createShared(AdminNotificationTemplates.paymentSuccess(order), false);
        }
    }

    private void rollbackOrderAllocations(Order order) {
        checkoutHelper.restoreStock(order);
        couponApplier.restoreCouponUsage(order.getCouponCode(), order.getShippingCouponCode());
    }

    private PaymentStatus parsePaymentStatus(String rawStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PAYMENT_STATUS, "Payment status is required");
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "COMPLETED", "SUCCESS", "SUCCEEDED", "PAID" -> PaymentStatus.COMPLETED;
            case "FAILED", "FAIL", "ERROR", "CANCELLED", "CANCELED" -> PaymentStatus.FAILED;
            case "REFUNDED", "REFUND" -> PaymentStatus.REFUNDED;
            case "PENDING", "PROCESSING" -> PaymentStatus.PENDING;
            default -> throw new BusinessException(
                    BusinessErrorCode.INVALID_PAYMENT_STATUS,
                    "Unsupported payment status: " + rawStatus)
                    .withMessageKey("error.unsupported_payment_status", rawStatus);
        };
    }

    private void validateMonetaryData(Order order, PaymentWebhookRequest request, PaymentStatus incomingStatus) {
        if (incomingStatus != PaymentStatus.COMPLETED && incomingStatus != PaymentStatus.REFUNDED) {
            return;
        }

        if (request.getAmount() == null) {
            throw new BusinessException(
                    BusinessErrorCode.WEBHOOK_PAYMENT_DATA_REQUIRED,
                    "Webhook amount is required for payment status: " + incomingStatus.name())
                    .withMessageKey("error.webhook_amount_required_for_status", incomingStatus.name());
        }
        String currency = normalizeCurrency(request.getCurrency());
        if (currency == null) {
            throw new BusinessException(
                    BusinessErrorCode.WEBHOOK_PAYMENT_DATA_REQUIRED,
                    "Webhook currency is required for payment status: " + incomingStatus.name())
                    .withMessageKey("error.webhook_currency_required_for_status", incomingStatus.name());
        }

        BigDecimal expectedAmount = nz(order.getTotalAmount()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal receivedAmount = request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (expectedAmount.compareTo(receivedAmount) != 0) {
            throw new BusinessException(
                    BusinessErrorCode.WEBHOOK_AMOUNT_MISMATCH,
                    "Webhook amount mismatch for order " + order.getOrderNumber()
                            + ". expected=" + expectedAmount + ", received=" + receivedAmount)
                    .withMessageKey("error.webhook_amount_mismatch", order.getOrderNumber(), expectedAmount, receivedAmount);
        }

        String expectedCurrency = normalizeCurrency(textSettingWithFallback("CURRENCY", "VND"));
        if (!expectedCurrency.equals(currency)) {
            throw new BusinessException(
                    BusinessErrorCode.WEBHOOK_CURRENCY_MISMATCH,
                    "Webhook currency mismatch for order " + order.getOrderNumber()
                            + ". expected=" + expectedCurrency + ", received=" + currency)
                    .withMessageKey("error.webhook_currency_mismatch", order.getOrderNumber(), expectedCurrency, currency);
        }
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : "UNKNOWN";
    }

    private String normalizeCurrency(String currency) {
        String normalized = trimToNull(currency);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private String textSettingWithFallback(String key, String fallback) {
        String value = settingService.getSettingValue(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void appendStatusHistory(Order order, OrderStatus status, String description) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .description(description)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    private void acquireLock(String lockKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
