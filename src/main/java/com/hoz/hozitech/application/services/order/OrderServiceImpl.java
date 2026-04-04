package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.constant.StatusConstant;
import com.hoz.hozitech.application.constant.PaginationConstant;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.constant.MailTemplate;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.application.services.flashsale.FlashSaleService;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.dtos.response.OrderStatusHistoryResponse;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
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

    @Value("${link.frontend:http://localhost:3000}")
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

            if (!StatusConstant.COUPON_ACTIVE.equalsIgnoreCase(coupon.getStatus())) {
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

            if (StatusConstant.DISCOUNT_PERCENTAGE.equalsIgnoreCase(coupon.getDiscountType())) {
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

        // Tính phí ship từ cài đặt hệ thống
        BigDecimal defaultFee = settingService.getSettingNumber("DEFAULT_SHIPPING_FEE");
        BigDecimal threshold = settingService.getSettingNumber("FREE_SHIPPING_THRESHOLD");
        BigDecimal shippingFee = (threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0)
                ? BigDecimal.ZERO : defaultFee;
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());

        // Validate: phương thức thanh toán phải đang được bật trong cài đặt
        String enabledKey = paymentMethod.name() + "_ENABLED";
        if (!settingService.getSettingBoolean(enabledKey)) {
            throw new IllegalArgumentException("Phương thức thanh toán " + paymentMethod.name() + " hiện không khả dụng");
        }
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

        // Log history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.PENDING)
                .description("Đơn hàng mới đã được tạo")
                .build();
        orderStatusHistoryRepository.save(history);

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
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStock(variant.getStock() + item.getQuantity());
            variantRepository.save(variant);
        }

        Order savedOrder = orderRepository.save(order);

        // Log history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.CANCELLED)
                .description("Người dùng đã huỷ đơn hàng")
                .build();
        orderStatusHistoryRepository.save(history);

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
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
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
            variables.put("ORDER_SUBTOTAL", formatPrice(order.getSubtotal()));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(order.getTotalAmount()));
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
            variables.put("ORDER_SUBTOTAL", formatPrice(order.getSubtotal()));
            variables.put("ORDER_DISCOUNT_AMOUNT",
                    order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                            ? formatPrice(order.getDiscountAmount()) : null);
            variables.put("ORDER_COUPON_CODE", order.getCouponCode());
            variables.put("ORDER_TOTAL", formatPrice(order.getTotalAmount()));
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
}
