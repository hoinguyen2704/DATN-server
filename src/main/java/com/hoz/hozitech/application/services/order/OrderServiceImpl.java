package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.DiscountType;
import com.hoz.hozitech.domain.enums.CouponStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.repositories.AddressRepository;
import com.hoz.hozitech.application.repositories.CartRepository;
import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.PaymentWebhookEventRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.constant.MailTemplate;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.application.services.flashsale.FlashSaleService;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.PaymentWebhookEvent;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.dtos.response.OrderStatusHistoryResponse;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.domain.enums.ProductStatus;
import com.hoz.hozitech.domain.enums.TaxMode;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final FlashSaleService flashSaleService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SettingService settingService;
    private final NotificationService notificationService;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${link.frontend}")
    private String frontendUrl;

    @Override
    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey) {
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

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            // Transaction-scoped lock guarantees one in-flight checkout per (user, key).
            acquirePgAdvisoryTransactionLock("checkout:" + userId + ":" + normalizedIdempotencyKey);
            Order existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, normalizedIdempotencyKey)
                    .orElse(null);
            if (existingOrder != null) {
                return buildCheckoutResponse(existingOrder);
            }
        }

        // Snapshot address as JSON
        String addressJson = snapshotAddress(address);

        // Build order items and calculate totals
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Set<UUID> checkedOutVariantIds = new LinkedHashSet<>();

        for (CheckoutRequest.CheckoutItem item : request.getItems()) {
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getVariantId())
                    .orElseThrow(() -> new BusinessException(
                            BusinessErrorCode.VARIANT_NOT_FOUND,
                            "Product variant not found: " + item.getVariantId()));
            validateVariantPurchasableForCheckout(variant);

            if (variant.getStock() < item.getQuantity()) {
                throw new BusinessException(
                        BusinessErrorCode.INSUFFICIENT_STOCK,
                        "Not enough stock for: " + variant.getVariantName());
            }

            // Check Flash Sale first
            BigDecimal flashPrice = flashSaleService.applyFlashSaleAndReduceStock(variant.getId(), item.getQuantity());
            BigDecimal unitPrice = (flashPrice != null) ? flashPrice : variant.getPrice();
            if (item.getExpectedUnitPrice() != null && item.getExpectedUnitPrice().compareTo(unitPrice) != 0) {
                throw new BusinessException(
                        BusinessErrorCode.PRICE_CHANGED,
                        "Price has changed for: " + variant.getVariantName() + ". Latest price: " + unitPrice);
            }
            
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productName(variant.getProduct().getName())
                    .variantName(variant.getVariantName())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(itemSubtotal)
                    .variant(variant)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(itemSubtotal);
            checkedOutVariantIds.add(variant.getId());

            // Reduce base stock
            variant.setStock(variant.getStock() - item.getQuantity());
            variantRepository.save(variant);
        }

        // Tính phí ship từ cài đặt hệ thống trước
        BigDecimal defaultFee = settingService.getSettingNumber("DEFAULT_SHIPPING_FEE");
        BigDecimal threshold = settingService.getSettingNumber("FREE_SHIPPING_THRESHOLD");
        BigDecimal shippingFee = (threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0)
                ? BigDecimal.ZERO : defaultFee;

        // Apply product coupon
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeForUpdate(request.getCouponCode())
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.INVALID_PRODUCT_COUPON, "Invalid product coupon code"));

            if (coupon.getCouponCategory() != CouponCategory.PRODUCT) {
                throw new BusinessException(BusinessErrorCode.INVALID_PRODUCT_COUPON, "Voucher is not a product voucher");
            }
            validateCoupon(coupon, subtotal);

            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (coupon.getMaxDiscountAmount() != null && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
            } else {
                discountAmount = coupon.getDiscountValue();
            }

            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        // Apply shipping coupon
        BigDecimal shippingDiscountAmount = BigDecimal.ZERO;
        if (request.getShippingCouponCode() != null && !request.getShippingCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeForUpdate(request.getShippingCouponCode())
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.INVALID_SHIPPING_COUPON, "Invalid shipping coupon code"));

            if (coupon.getCouponCategory() != CouponCategory.SHIPPING) {
                throw new BusinessException(BusinessErrorCode.INVALID_SHIPPING_COUPON, "Voucher is not a freeship voucher");
            }
            validateCoupon(coupon, subtotal);

            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                shippingDiscountAmount = shippingFee.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (coupon.getMaxDiscountAmount() != null && shippingDiscountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    shippingDiscountAmount = coupon.getMaxDiscountAmount();
                }
            } else {
                shippingDiscountAmount = coupon.getDiscountValue();
            }
            
            // Limit shipping discount to actual shipping fee
            if (shippingDiscountAmount == null || shippingDiscountAmount.compareTo(BigDecimal.ZERO) == 0) {
                 shippingDiscountAmount = shippingFee; // If 0 or null, freeship 100%
            }
            if (shippingDiscountAmount.compareTo(shippingFee) > 0) {
                shippingDiscountAmount = shippingFee;
            }

            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        BigDecimal productBase = subtotal.subtract(discountAmount);
        if (productBase.compareTo(BigDecimal.ZERO) < 0) productBase = BigDecimal.ZERO;

        BigDecimal shippingBase = shippingFee.subtract(shippingDiscountAmount);
        if (shippingBase.compareTo(BigDecimal.ZERO) < 0) shippingBase = BigDecimal.ZERO;

        TaxSnapshot taxSnapshot = calculateTaxSnapshot(productBase, shippingBase);
        BigDecimal totalAmount = taxSnapshot.totalAmount();

        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        // Validate: phương thức thanh toán phải đang được bật trong cài đặt
        String enabledKey = paymentMethod.name() + "_ENABLED";
        if (!settingService.getSettingBoolean(enabledKey)) {
            throw new BusinessException(
                    BusinessErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    "Phương thức thanh toán " + paymentMethod.name() + " hiện không khả dụng");
        }
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .idempotencyKey(normalizedIdempotencyKey)
                .shippingAddressJson(addressJson)
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
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PENDING)
                .user(user)
                .couponCode(request.getCouponCode())
                .shippingCouponCode(request.getShippingCouponCode())
                .orderItems(new ArrayList<>())
                .build();

        // Link order items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getOrderItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        // Log history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.PENDING)
                .description("Đơn hàng mới đã được tạo")
                .build();
        orderStatusHistoryRepository.save(history);

        // Clear cart items after successful checkout
        if (!checkedOutVariantIds.isEmpty()) {
            cartRepository.deleteByUserIdAndVariantIdIn(userId, checkedOutVariantIds);
        }

        OrderResponse response = buildCheckoutResponse(savedOrder);

        // Send order created email
        sendOrderCreatedEmail(savedOrder, user, address);
        notificationService.createForUser(
                userId,
                "Đặt hàng thành công",
                "Đơn hàng " + savedOrder.getOrderNumber() + " đã được tạo thành công.",
                "ORDER",
                savedOrder.getId()
        );

        return response;
    }

    @Override
    @Transactional
    public OrderResponse handlePaymentWebhook(PaymentWebhookRequest request, String idempotencyKey) {
        String resolvedIdempotencyKey = resolveWebhookIdempotencyKey(request, idempotencyKey);
        acquirePgAdvisoryTransactionLock("payment-webhook:" + resolvedIdempotencyKey);

        PaymentWebhookEvent existingEvent = paymentWebhookEventRepository.findByIdempotencyKey(resolvedIdempotencyKey)
                .orElse(null);
        if (existingEvent != null) {
            if (existingEvent.getOrder() != null) {
                return mapToResponse(existingEvent.getOrder());
            }
            Order existingOrder = orderRepository.findByOrderNumber(existingEvent.getOrderNumber())
                    .orElseThrow(() -> new BusinessException(
                            BusinessErrorCode.WEBHOOK_ORDER_NOT_FOUND,
                            "Order not found for webhook event: " + existingEvent.getOrderNumber()));
            return mapToResponse(existingOrder);
        }

        PaymentStatus incomingStatus = parseWebhookPaymentStatus(request.getPaymentStatus());
        Order order = orderRepository.findByOrderNumberForUpdate(request.getOrderNumber())
                .orElseThrow(() -> new BusinessException(
                        BusinessErrorCode.WEBHOOK_ORDER_NOT_FOUND,
                        "Order not found: " + request.getOrderNumber()));

        applyPaymentWebhookTransition(order, incomingStatus);
        Order savedOrder = orderRepository.save(order);

        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .idempotencyKey(resolvedIdempotencyKey)
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

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumberForAdmin(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, String status, String keyword, int page, int size) {
        var pageable = PaginationConstant.of(page, size);

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Specification<Order> spec = OrderSpecification.filter(userId, orderStatus, null, null, keyword);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return PageResponse.of(orders.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Restore stock
        restoreStockForOrder(order);

        Order savedOrder = orderRepository.save(order);

        // Log history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.CANCELLED)
                .description("Người dùng đã huỷ đơn hàng")
                .build();
        orderStatusHistoryRepository.save(history);
        notificationService.createForUser(
                userId,
                "Đơn hàng đã bị huỷ",
                "Đơn hàng " + savedOrder.getOrderNumber() + " đã được huỷ theo yêu cầu của bạn.",
                "ORDER",
                savedOrder.getId()
        );

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(String status, String keyword, int page, int size) {
        var pageable = PaginationConstant.of(page, size);

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Specification<Order> spec = OrderSpecification.filter(null, orderStatus, null, null, keyword);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return PageResponse.of(orders.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        OrderStatus oldStatus = order.getOrderStatus();

        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.SHIPPED) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }

        Order updatedOrder = orderRepository.save(order);

        // Log history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(updatedOrder)
                .status(newStatus)
                .description(getDefaultDescriptionForStatus(newStatus))
                .build();
        orderStatusHistoryRepository.save(history);
        if (oldStatus != newStatus) {
            notificationService.createForUser(
                    updatedOrder.getUser().getId(),
                    "Cập nhật đơn hàng",
                    "Đơn hàng " + updatedOrder.getOrderNumber() + " đã chuyển sang trạng thái " + getStatusLabel(newStatus) + ".",
                    "ORDER",
                    updatedOrder.getId()
            );
        }

        // Send shipped email notification
        if (newStatus == OrderStatus.SHIPPED) {
            sendOrderShippedEmail(updatedOrder);
        }

        return mapToResponse(updatedOrder);
    }

    // --- Private helpers ---

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    String imageUrl = null;
                    String sku = null;
                    if (item.getVariant() != null) {
                        sku = item.getVariant().getSku();
                        // Try variant-specific image first, then product primary image
                        if (item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty()) {
                            imageUrl = item.getVariant().getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                    .findFirst()
                                    .map(ProductImage::getImageUrl)
                                    .orElse(item.getVariant().getImages().get(0).getImageUrl());
                        } else if (item.getVariant().getProduct() != null
                                && item.getVariant().getProduct().getImages() != null
                                && !item.getVariant().getProduct().getImages().isEmpty()) {
                            imageUrl = item.getVariant().getProduct().getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                    .findFirst()
                                    .map(ProductImage::getImageUrl)
                                    .orElse(item.getVariant().getProduct().getImages().get(0).getImageUrl());
                        }
                    }
                    return OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .productId(item.getVariant() != null && item.getVariant().getProduct() != null
                                ? item.getVariant().getProduct().getId()
                                : null)
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .imageUrl(imageUrl)
                        .sku(sku)
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build();
                })
                .collect(Collectors.toList());

        // Extract customer info from user
        User user = order.getUser();
        String customerName = user != null ? user.getFullName() : null;
        String customerEmail = user != null ? user.getEmail() : null;
        String customerPhone = user != null ? user.getPhoneNumber() : null;

        // Map status histories
        List<OrderStatusHistoryResponse> historyResponses = order.getStatusHistories().stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus().name())
                        .description(history.getDescription())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Format shipping address from JSON to readable string
        String formattedAddress = formatShippingAddress(order.getShippingAddressJson());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .shippingDiscountAmount(order.getShippingDiscountAmount())
                .taxPercent(nz(order.getTaxPercent()))
                .taxMode(order.getTaxMode() != null ? order.getTaxMode().name() : TaxMode.INCLUDED.name())
                .taxableAmount(nz(order.getTaxableAmount()))
                .taxAmount(nz(order.getTaxAmount()))
                .taxApplyOnShipping(order.getTaxApplyOnShipping() != null ? order.getTaxApplyOnShipping() : Boolean.FALSE)
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .shippingCouponCode(order.getShippingCouponCode())
                .note(order.getNote())
                .shippingAddress(formattedAddress)
                .trackingCode(order.getTrackingCode())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .statusHistories(historyResponses)
                .build();
    }

    private OrderResponse buildCheckoutResponse(Order order) {
        OrderResponse response = mapToResponse(order);
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            response.setPaymentUrl("https://payment.hozitech.com/pay/" + order.getOrderNumber());
        }
        return response;
    }

    private String normalizeIdempotencyKey(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized;
    }

    private String resolveWebhookIdempotencyKey(PaymentWebhookRequest request, String webhookIdHeader) {
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

    private void acquirePgAdvisoryTransactionLock(String lockKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }

    private void applyPaymentWebhookTransition(Order order, PaymentStatus incomingStatus) {
        PaymentStatus currentStatus = order.getPaymentStatus();

        if (currentStatus == incomingStatus) {
            return;
        }
        // Do not downgrade completed payments unless explicit REFUNDED.
        if (currentStatus == PaymentStatus.COMPLETED && incomingStatus != PaymentStatus.REFUNDED) {
            return;
        }
        // Once refunded, keep terminal state.
        if (currentStatus == PaymentStatus.REFUNDED) {
            return;
        }
        // Prevent re-opening a failed payment as completed without manual operation.
        if (currentStatus == PaymentStatus.FAILED && incomingStatus == PaymentStatus.COMPLETED) {
            return;
        }

        order.setPaymentStatus(incomingStatus);

        if (incomingStatus == PaymentStatus.FAILED) {
            if (order.getOrderStatus() != OrderStatus.CANCELLED
                    && order.getOrderStatus() != OrderStatus.SHIPPED
                    && order.getOrderStatus() != OrderStatus.RETURNED) {
                restoreStockForOrder(order);
                order.setOrderStatus(OrderStatus.CANCELLED);
                appendOrderStatusHistory(order, OrderStatus.CANCELLED, "Thanh toán thất bại từ webhook, đơn đã huỷ");
            }
            notificationService.createForUser(
                    order.getUser().getId(),
                    "Thanh toán thất bại",
                    "Đơn hàng " + order.getOrderNumber() + " thanh toán không thành công.",
                    "ORDER",
                    order.getId()
            );
            return;
        }

        if (incomingStatus == PaymentStatus.REFUNDED) {
            if (order.getOrderStatus() != OrderStatus.RETURNED) {
                order.setOrderStatus(OrderStatus.RETURNED);
                appendOrderStatusHistory(order, OrderStatus.RETURNED, "Đơn hàng đã được hoàn tiền qua webhook");
            }
            notificationService.createForUser(
                    order.getUser().getId(),
                    "Đã hoàn tiền",
                    "Đơn hàng " + order.getOrderNumber() + " đã được hoàn tiền.",
                    "ORDER",
                    order.getId()
            );
            return;
        }

        if (incomingStatus == PaymentStatus.COMPLETED) {
            notificationService.createForUser(
                    order.getUser().getId(),
                    "Thanh toán thành công",
                    "Đơn hàng " + order.getOrderNumber() + " đã được thanh toán.",
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            if (item.getVariant() == null) continue;
            UUID variantId = item.getVariant().getId();
            ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElse(item.getVariant());
            variant.setStock(variant.getStock() + item.getQuantity());
            variantRepository.save(variant);
        }
    }

    private void appendOrderStatusHistory(Order order, OrderStatus status, String description) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .description(description)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    private PaymentStatus parseWebhookPaymentStatus(String rawStatus) {
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
                    "Unsupported payment status: " + rawStatus);
        };
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        if (normalized == null) {
            return "UNKNOWN";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_ACTIVE, "Coupon is not active");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(LocalDateTime.now())) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_STARTED, "Coupon is not valid yet");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BusinessErrorCode.COUPON_EXPIRED, "Coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException(BusinessErrorCode.COUPON_USAGE_LIMIT_EXCEEDED, "Coupon usage limit exceeded");
        }
        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BusinessException(BusinessErrorCode.COUPON_MIN_ORDER_NOT_MET, "Order does not meet minimum value for coupon");
        }
    }

    private void validateVariantPurchasableForCheckout(ProductVariant variant) {
        if (variant.getProduct() == null || variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_AVAILABLE, "Product is not available for purchase");
        }
        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new BusinessException(BusinessErrorCode.VARIANT_NOT_AVAILABLE, "Product variant is not available for purchase");
        }
    }

    private PaymentMethod parsePaymentMethod(String paymentMethodRaw) {
        try {
            return PaymentMethod.valueOf(paymentMethodRaw.toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_PAYMENT_METHOD, "Invalid payment method: " + paymentMethodRaw);
        }
    }

    private TaxSnapshot calculateTaxSnapshot(BigDecimal productBase, BigDecimal shippingBase) {
        BigDecimal safeProductBase = nz(productBase);
        BigDecimal safeShippingBase = nz(shippingBase);
        BigDecimal netTotal = safeProductBase.add(safeShippingBase);
        if (netTotal.compareTo(BigDecimal.ZERO) < 0) netTotal = BigDecimal.ZERO;

        boolean taxEnabled = boolSettingWithFallback("TAX_ENABLED", true);
        TaxMode taxMode = parseTaxMode(textSettingWithFallback("TAX_MODE", TaxMode.INCLUDED.name()));
        boolean taxApplyOnShipping = boolSettingWithFallback("TAX_APPLY_ON_SHIPPING", true);
        BigDecimal taxPercent = settingService.getSettingNumber("DEFAULT_TAX_PERCENT");
        if (taxPercent == null || taxPercent.compareTo(BigDecimal.ZERO) < 0) {
            taxPercent = BigDecimal.ZERO;
        }

        BigDecimal taxableAmount = safeProductBase;
        if (taxApplyOnShipping) {
            taxableAmount = taxableAmount.add(safeShippingBase);
        }
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = netTotal;

        if (taxEnabled && taxPercent.compareTo(BigDecimal.ZERO) > 0 && taxableAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (taxMode == TaxMode.EXCLUDED) {
                taxAmount = taxableAmount
                        .multiply(taxPercent)
                        .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
                totalAmount = netTotal.add(taxAmount);
            } else {
                BigDecimal denominator = ONE_HUNDRED.add(taxPercent);
                if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                    taxAmount = taxableAmount
                            .multiply(taxPercent)
                            .divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
                }
                totalAmount = netTotal;
            }
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        return new TaxSnapshot(
                taxPercent,
                taxMode,
                taxApplyOnShipping,
                taxableAmount,
                taxAmount,
                totalAmount
        );
    }

    private TaxMode parseTaxMode(String mode) {
        if (mode == null || mode.isBlank()) return TaxMode.INCLUDED;
        try {
            return TaxMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TaxMode.INCLUDED;
        }
    }

    private String textSettingWithFallback(String key, String fallback) {
        String value = settingService.getSettingValue(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private boolean boolSettingWithFallback(String key, boolean fallback) {
        String value = settingService.getSettingValue(key);
        if (value == null || value.isBlank()) return fallback;
        return "true".equalsIgnoreCase(value);
    }

    @SuppressWarnings("unchecked")
    private String formatShippingAddress(String addressJson) {
        if (addressJson == null || addressJson.isBlank()) return "";
        try {
            var map = objectMapper.readValue(addressJson, java.util.Map.class);
            String fullName = (String) map.getOrDefault("fullName", "");
            String phone = (String) map.getOrDefault("phoneNumber", "");
            String detail = (String) map.getOrDefault("detailAddress", "");
            String ward = (String) map.getOrDefault("ward", "");
            String district = (String) map.getOrDefault("district", "");
            String province = (String) map.getOrDefault("province", "");

            StringBuilder sb = new StringBuilder();
            if (!fullName.isEmpty()) sb.append(fullName);
            if (!phone.isEmpty()) sb.append(" - ").append(phone);
            if (!detail.isEmpty()) sb.append(", ").append(detail);
            if (!ward.isEmpty()) sb.append(", ").append(ward);
            if (!district.isEmpty()) sb.append(", ").append(district);
            if (!province.isEmpty()) sb.append(", ").append(province);
            return sb.toString();
        } catch (Exception e) {
            return addressJson; // Fallback to raw JSON
        }
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + date + "-" + random;
    }

    private String snapshotAddress(Address address) {
        try {
            var map = new java.util.LinkedHashMap<String, String>();
            map.put("fullName", address.getFullName());
            map.put("phoneNumber", address.getPhoneNumber());
            map.put("province", address.getProvince());
            map.put("district", address.getDistrict());
            map.put("ward", address.getWard());
            map.put("detailAddress", address.getDetailAddress());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize address", e);
            return "{}";
        }
    }

    // --- Email helpers ---

    private void sendOrderCreatedEmail(Order order, User user, Address address) {
        try {
            String customerEmail = user.getEmail();
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = new HashMap<>();
            variables.put("CUSTOMER_NAME", user.getFullName());
            variables.put("ORDER_NUMBER", order.getOrderNumber());
            variables.put("ORDER_DATE", order.getCreatedAt() != null
                    ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            variables.put("CUSTOMER_FULL_NAME", address.getFullName());
            variables.put("CUSTOMER_PHONE", address.getPhoneNumber());
            variables.put("CUSTOMER_ADDRESS", buildFullAddress(address));
            
            OrderResponse response = mapToResponse(order);
            variables.put("ORDER_ITEMS", response.getItems());
            variables.put("ORDER_SUBTOTAL", formatPrice(nz(order.getSubtotal())));
            variables.put("ORDER_SHIPPING_FEE", formatPrice(nz(order.getShippingFee())));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    nz(order.getDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_SHIPPING_DISCOUNT_AMOUNT",
                    nz(order.getShippingDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getShippingDiscountAmount()) : null);
            variables.put("ORDER_TAX_LABEL", buildTaxLabel(order));
            variables.put("ORDER_TAX_AMOUNT",
                    nz(order.getTaxAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getTaxAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(nz(order.getTotalAmount())));
            variables.put("ORDER_LINK", frontendUrl + "/user/orders/" + order.getOrderNumber());

            emailService.sendTemplateMail(customerEmail,
                    "Đơn hàng " + order.getOrderNumber() + " đã tạo thành công - HoziTech",
                    MailTemplate.ORDER_CREATED, variables);
        } catch (Exception e) {
            log.error("Failed to send order created email for order {}", order.getOrderNumber(), e);
        }
    }

    private void sendOrderShippedEmail(Order order) {
        try {
            User user = order.getUser();
            String customerEmail = user.getEmail();
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = new HashMap<>();
            variables.put("CUSTOMER_NAME", user.getFullName());
            variables.put("ORDER_NUMBER", order.getOrderNumber());
            variables.put("SHIPPED_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
            OrderResponse response = mapToResponse(order);
            variables.put("ORDER_ITEMS", response.getItems());
            variables.put("ORDER_SUBTOTAL", formatPrice(nz(order.getSubtotal())));
            variables.put("ORDER_SHIPPING_FEE", formatPrice(nz(order.getShippingFee())));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    nz(order.getDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_SHIPPING_DISCOUNT_AMOUNT",
                    nz(order.getShippingDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getShippingDiscountAmount()) : null);
            variables.put("ORDER_TAX_LABEL", buildTaxLabel(order));
            variables.put("ORDER_TAX_AMOUNT",
                    nz(order.getTaxAmount()).compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getTaxAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(nz(order.getTotalAmount())));
            variables.put("ORDER_LINK", frontendUrl + "/user/orders/" + order.getOrderNumber());

            emailService.sendTemplateMail(customerEmail,
                    "Đơn hàng " + order.getOrderNumber() + " đã giao thành công - HoziTech",
                    MailTemplate.ORDER_SHIPPED, variables);
        } catch (Exception e) {
            log.error("Failed to send order shipped email for order {}", order.getOrderNumber(), e);
        }
    }

    private String buildFullAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getDetailAddress() != null) sb.append(address.getDetailAddress());
        if (address.getWard() != null) sb.append(", ").append(address.getWard());
        if (address.getDistrict() != null) sb.append(", ").append(address.getDistrict());
        if (address.getProvince() != null) sb.append(", ").append(address.getProvince());
        return sb.toString();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        return String.format("%,.0f", price);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildTaxLabel(Order order) {
        BigDecimal percent = nz(order.getTaxPercent()).stripTrailingZeros();
        String percentText = percent.scale() <= 0 ? percent.toPlainString() : percent.toPlainString();
        if (order.getTaxMode() == TaxMode.EXCLUDED) {
            return "Thuế VAT (" + percentText + "%)";
        }
        return "Thuế VAT (" + percentText + "%, đã gồm)";
    }

    private record TaxSnapshot(
            BigDecimal taxPercent,
            TaxMode taxMode,
            boolean taxApplyOnShipping,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {}
    
    private String getDefaultDescriptionForStatus(OrderStatus status) {
        switch (status) {
            case OrderStatus.PENDING: return "Đơn hàng đang chờ xác nhận";
            case OrderStatus.CONFIRMED: return "Người gửi đang chuẩn bị hàng";
            case OrderStatus.PROCESSING: return "Đơn hàng đang được đóng gói";
            case OrderStatus.SHIPPING: return "Đơn hàng đã được giao cho đơn vị vận chuyển";
            case OrderStatus.SHIPPED: return "Đơn hàng đang trên đường giao đến bạn";
            case OrderStatus.CANCELLED: return "Đơn hàng đã bị huỷ";
            case OrderStatus.RETURNED: return "Đơn hàng đã được hoàn trả";
            default: return "Cập nhật trạng thái đơn hàng";
        }
    }

    private String getStatusLabel(OrderStatus status) {
        switch (status) {
            case OrderStatus.PENDING: return "chờ xác nhận";
            case OrderStatus.CONFIRMED: return "đã xác nhận";
            case OrderStatus.PROCESSING: return "đang xử lý";
            case OrderStatus.SHIPPING: return "đang giao";
            case OrderStatus.SHIPPED: return "đã giao";
            case OrderStatus.CANCELLED: return "đã huỷ";
            case OrderStatus.RETURNED: return "đã hoàn trả";
            default: return status.name();
        }
    }
}
