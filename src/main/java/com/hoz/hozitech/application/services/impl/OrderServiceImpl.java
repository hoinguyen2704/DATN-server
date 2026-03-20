package com.hoz.hozitech.application.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.repositories.AddressRepository;
import com.hoz.hozitech.application.repositories.CartRepository;
import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.OrderItemRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.constant.MailTemplate;
import com.hoz.hozitech.application.services.EmailService;
import com.hoz.hozitech.application.services.FlashSaleService;
import com.hoz.hozitech.application.services.OrderService;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final FlashSaleService flashSaleService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${link.frontend:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        // Snapshot address as JSON
        String addressJson = snapshotAddress(address);

        // Build order items and calculate totals
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutRequest.CheckoutItem item : request.getItems()) {
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + item.getVariantId()));

            if (variant.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for: " + variant.getVariantName());
            }

            // Check Flash Sale first
            BigDecimal flashPrice = flashSaleService.applyFlashSaleAndReduceStock(variant.getId(), item.getQuantity());
            BigDecimal unitPrice = (flashPrice != null) ? flashPrice : variant.getPrice();
            
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

            // Reduce base stock
            variant.setStock(variant.getStock() - item.getQuantity());
            variantRepository.save(variant);
        }

        // Apply coupon
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

            if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
                throw new IllegalArgumentException("Coupon is not active");
            }
            if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Coupon has expired");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new IllegalArgumentException("Coupon usage limit exceeded");
            }
            if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new IllegalArgumentException("Order does not meet minimum value for coupon");
            }

            if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
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

        BigDecimal shippingFee = BigDecimal.ZERO; // Can be calculated based on address later
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .shippingAddressJson(addressJson)
                .note(request.getNote())
                .orderStatus(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PENDING)
                .user(user)
                .couponCode(request.getCouponCode())
                .orderItems(new ArrayList<>())
                .build();

        // Link order items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getOrderItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        // Clear cart items after successful checkout
        cartRepository.deleteAllByUserId(userId);

        OrderResponse response = mapToResponse(savedOrder);

        // Generate payment URL for online payments
        if (paymentMethod != PaymentMethod.COD) {
            response.setPaymentUrl("https://payment.hozitech.com/pay/" + savedOrder.getOrderNumber());
        }

        // Send order created email
        sendOrderCreatedEmail(savedOrder, user, address);

        return response;
    }

    @Override
    public OrderResponse getOrderByNumber(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return mapToResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(UUID userId, String status, int page, int size) {
        var pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Specification<Order> spec = OrderSpecification.filter(userId, orderStatus, null, null, null);
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
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStock(variant.getStock() + item.getQuantity());
            variantRepository.save(variant);
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    public PageResponse<OrderResponse> getAllOrders(String status, String keyword, int page, int size) {
        var pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

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

        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.SHIPPED) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }

        Order updatedOrder = orderRepository.save(order);

        // Send shipped email notification
        if (newStatus == OrderStatus.SHIPPED) {
            sendOrderShippedEmail(updatedOrder);
        }

        return mapToResponse(updatedOrder);
    }

    // --- Private helpers ---

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .note(order.getNote())
                .shippingAddress(order.getShippingAddressJson())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + date + "-" + random;
    }

    private String snapshotAddress(Address address) {
        return "{" +
                "\"fullName\":\"" + escapeJson(address.getFullName()) + "\"," +
                "\"phoneNumber\":\"" + escapeJson(address.getPhoneNumber()) + "\"," +
                "\"province\":\"" + escapeJson(address.getProvince()) + "\"," +
                "\"district\":\"" + escapeJson(address.getDistrict()) + "\"," +
                "\"ward\":\"" + escapeJson(address.getWard()) + "\"," +
                "\"detailAddress\":\"" + escapeJson(address.getDetailAddress()) + "\"" +
                "}";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
            variables.put("ORDER_ITEMS", order.getOrderItems());
            variables.put("ORDER_SUBTOTAL", formatPrice(order.getSubtotal()));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(order.getTotalAmount()));
            variables.put("ORDER_LINK", frontendUrl + "/order/detail/" + order.getOrderNumber());

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
            variables.put("ORDER_ITEMS", order.getOrderItems());
            variables.put("ORDER_SUBTOTAL", formatPrice(order.getSubtotal()));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(order.getTotalAmount()));
            variables.put("ORDER_LINK", frontendUrl + "/order/detail/" + order.getOrderNumber());

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
}
