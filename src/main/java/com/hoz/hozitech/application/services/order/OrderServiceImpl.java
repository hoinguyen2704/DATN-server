package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.AddressRepository;
import com.hoz.hozitech.application.repositories.CartRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.notification.UserNotificationTemplates;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import com.hoz.hozitech.domain.dtos.response.AdminOrderListItemResponse;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.hoz.hozitech.application.services.order.OrderUtils.normalizeIdempotencyKey;
import static com.hoz.hozitech.application.services.order.OrderUtils.trimToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(
                    OrderStatus.PROCESSING,
                    OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(
                    OrderStatus.SHIPPING,
                    OrderStatus.CANCELLED),
            OrderStatus.SHIPPING, EnumSet.of(
                    OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.RETURNED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SettingService settingService;
    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;

    // Extracted helpers
    private final OrderCheckoutHelper checkoutHelper;
    private final CouponApplier couponApplier;
    private final TaxCalculator taxCalculator;
    private final OrderResponseMapper responseMapper;
    private final OrderEmailSender emailSender;
    private final PaymentWebhookHandler webhookHandler;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "User not found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ADDRESS_NOT_FOUND, "Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.ADDRESS_NOT_OWNED, "Address does not belong to user");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.EMPTY_CHECKOUT_ITEMS, "Checkout items must not be empty");
        }

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey == null) {
            throw new BusinessException(BusinessErrorCode.IDEMPOTENCY_KEY_REQUIRED, "Idempotency key is required for checkout");
        }

        // Transaction-scoped lock guarantees one in-flight checkout per (user, key).
        acquirePgAdvisoryTransactionLock("checkout:" + userId + ":" + normalizedKey);
        Order existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, normalizedKey).orElse(null);
        if (existingOrder != null) {
            return responseMapper.buildCheckoutResponse(existingOrder, ipAddress);
        }

        // Build order items, validate variants, reduce stock
        OrderCheckoutHelper.CheckoutItemsResult itemsResult = checkoutHelper.buildOrderItems(request.getItems());
        BigDecimal subtotal = itemsResult.getSubtotal();

        // Calculate shipping fee
        BigDecimal shippingFee = checkoutHelper.calculateShippingFee(subtotal);

        // Apply coupons
        BigDecimal discountAmount = couponApplier.applyProductCoupon(
                request.getCouponCode(), subtotal, itemsResult.getCheckedOutProductIds(), userId);
        BigDecimal shippingDiscountAmount = couponApplier.applyShippingCoupon(
                request.getShippingCouponCode(), shippingFee, subtotal, itemsResult.getCheckedOutProductIds(), userId);

        // Calculate tax
        BigDecimal productBase = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal shippingBase = shippingFee.subtract(shippingDiscountAmount).max(BigDecimal.ZERO);
        TaxCalculator.TaxSnapshot taxSnapshot = taxCalculator.calculate(productBase, shippingBase);

        // Validate payment method
        PaymentMethod paymentMethod = checkoutHelper.parsePaymentMethod(request.getPaymentMethod());
        String enabledKey = paymentMethod.name() + "_ENABLED";
        if (!settingService.getSettingBoolean(enabledKey)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    "Phương thức thanh toán " + paymentMethod.name() + " hiện không khả dụng");
        }

        // Build and save order
        Order order = Order.builder()
                .orderNumber(checkoutHelper.generateOrderNumber())
                .idempotencyKey(normalizedKey)
                .shippingAddressJson(checkoutHelper.snapshotAddress(address))
                .note(request.getNote())
                .orderStatus(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .shippingDiscountAmount(shippingDiscountAmount)
                .taxPercent(taxSnapshot.taxPercent())
                .taxMode(taxSnapshot.taxMode())
                .taxableAmount(taxSnapshot.taxableAmount())
                .taxAmount(taxSnapshot.taxAmount())
                .taxApplyOnShipping(taxSnapshot.taxApplyOnShipping())
                .totalAmount(taxSnapshot.totalAmount())
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .user(user)
                .couponCode(request.getCouponCode())
                .shippingCouponCode(request.getShippingCouponCode())
                .orderItems(new ArrayList<>())
                .build();

        for (OrderItem item : itemsResult.getOrderItems()) {
            item.setOrder(order);
            order.getOrderItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        // Log history
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(savedOrder).status(OrderStatus.PENDING).description("Đơn hàng mới đã được tạo").build());

        // Clear cart
        if (!itemsResult.getCheckedOutVariantIds().isEmpty()) {
            cartRepository.deleteByUserIdAndVariantIdIn(userId, itemsResult.getCheckedOutVariantIds());
        }

        // Send email & notification
        emailSender.sendOrderCreatedEmail(savedOrder, user, address);
        notificationService.createForUser(userId, UserNotificationTemplates.orderCreated(savedOrder));
        adminNotificationService.createShared(AdminNotificationTemplates.orderCreated(savedOrder), false);

        return responseMapper.buildCheckoutResponse(savedOrder, ipAddress);
    }

    @Override
    @Transactional
    public OrderResponse handlePaymentWebhook(PaymentWebhookRequest request, String idempotencyKey) {
        return webhookHandler.handle(request, idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Order does not belong to user");
        }
        return responseMapper.mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumberForAdmin(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new InvalidParamException("Order not found"));
        return responseMapper.mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, String status, String keyword, int page, int size) {
        var pageable = PaginationConstant.of(page, size);
        OrderStatus orderStatus = parseNullableOrderStatus(status);
        Specification<Order> spec = OrderSpecification.filter(userId, orderStatus, null, null, keyword);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return PageResponse.of(orders.map(responseMapper::mapToResponse));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Order does not belong to user");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidParamException("Only pending orders can be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        checkoutHelper.restoreStock(order);
        couponApplier.restoreCouponUsage(order.getCouponCode(), order.getShippingCouponCode());

        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(savedOrder).status(OrderStatus.CANCELLED).description("Người dùng đã huỷ đơn hàng").build());
        notificationService.createForUser(userId, UserNotificationTemplates.orderCancelled(savedOrder));

        return responseMapper.mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderListItemResponse> getAllOrders(String status, String keyword, int page, int size, String sortBy, String sortDir) {
        var pageable = PaginationConstant.of(page, size, resolveAdminOrderSort(sortBy, sortDir));
        OrderStatus orderStatus = parseNullableOrderStatus(status);
        Specification<Order> spec = OrderSpecification.adminListFilter(orderStatus, keyword);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return PageResponse.of(orders.map(this::mapToAdminListItem));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        OrderStatus oldStatus = order.getOrderStatus();
        OrderStatus newStatus = parseOrderStatus(status);

        if (oldStatus == newStatus) {
            return responseMapper.mapToResponse(order);
        }

        validateOrderStatusTransition(oldStatus, newStatus);
        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            checkoutHelper.restoreStock(order);
            couponApplier.restoreCouponUsage(order.getCouponCode(), order.getShippingCouponCode());
        }
        if (newStatus == OrderStatus.SHIPPED) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }

        Order updatedOrder = orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(updatedOrder).status(newStatus).description(newStatus.getDescription()).build());

        if (oldStatus != newStatus) {
            notificationService.createForUser(updatedOrder.getUser().getId(), UserNotificationTemplates.orderStatusChanged(updatedOrder));
        }

        if (newStatus == OrderStatus.SHIPPED) {
            emailSender.sendOrderShippedEmail(updatedOrder);
        }

        return responseMapper.mapToResponse(updatedOrder);
    }

    // --- Private helpers ---

    private OrderStatus parseNullableOrderStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return null;
        return parseOrderStatus(rawStatus);
    }

    private AdminOrderListItemResponse mapToAdminListItem(Order order) {
        User user = order.getUser();
        return AdminOrderListItemResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .customerName(user != null ? user.getFullName() : null)
                .customerEmail(user != null ? user.getEmail() : null)
                .customerPhone(user != null ? user.getPhoneNumber() : null)
                .itemCount(order.getItemCount() != null ? order.getItemCount() : 0)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private Sort resolveAdminOrderSort(String sortBy, String sortDir) {
        Sort.Direction direction = Sort.Direction.ASC.name().equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String resolvedField = switch (sortBy == null ? "" : sortBy) {
            case "orderNumber" -> "orderNumber";
            case "createdAt" -> "createdAt";
            case "itemCount" -> "itemCount";
            case "totalAmount" -> "totalAmount";
            case "paymentMethod" -> "paymentMethod";
            case "orderStatus" -> "orderStatus";
            default -> "createdAt";
        };

        if ("createdAt".equals(resolvedField)) {
            return Sort.by(
                    new Sort.Order(direction, resolvedField),
                    Sort.Order.desc("id"));
        }

        return Sort.by(
                new Sort.Order(direction, resolvedField),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));
    }

    private OrderStatus parseOrderStatus(String rawStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_ORDER_STATUS, "Order status is required");
        }
        try {
            return OrderStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_ORDER_STATUS, "Unsupported order status: " + rawStatus);
        }
    }

    private void validateOrderStatusTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        Set<OrderStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(
                currentStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowedStatuses.contains(nextStatus)) {
            throw new BusinessException(BusinessErrorCode.ORDER_STATUS_TRANSITION_NOT_ALLOWED,
                    "Cannot transition order status from " + currentStatus.name() + " to " + nextStatus.name());
        }
    }

    private void acquirePgAdvisoryTransactionLock(String lockKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
