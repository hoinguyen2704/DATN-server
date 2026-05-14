package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.nz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.services.payment.VnpayPaymentService;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.dtos.response.OrderStatusHistoryResponse;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.TaxMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class OrderResponseMapper {

    private final ObjectMapper objectMapper;
    private final VnpayPaymentService vnpayPaymentService;

    OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    String imageUrl = resolveOrderItemImageUrl(item);
                    String sku = null;
                    String productSlug = null;
                    if (item.getVariant() != null) {
                        sku = item.getVariant().getSku();
                        if (item.getVariant().getProduct() != null) {
                            productSlug = item.getVariant().getProduct().getSlug();
                        }
                    }
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                            .productId(item.getVariant() != null && item.getVariant().getProduct() != null
                                    ? item.getVariant().getProduct().getId()
                                    : null)
                            .productSlug(productSlug)
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

        User user = order.getUser();
        String customerName = user != null ? user.getFullName() : null;
        String customerEmail = user != null ? user.getEmail() : null;
        String customerPhone = user != null ? user.getPhoneNumber() : null;

        List<OrderStatusHistoryResponse> historyResponses = order.getStatusHistories().stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus().name())
                        .description(history.getDescription())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

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

    OrderResponse mapToListResponse(Order order, Map<UUID, String> imageByProductId) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    UUID productId = item.getVariant() != null && item.getVariant().getProduct() != null
                            ? item.getVariant().getProduct().getId()
                            : null;
                    String productSlug = item.getVariant() != null && item.getVariant().getProduct() != null
                            ? item.getVariant().getProduct().getSlug()
                            : null;
                    String sku = item.getVariant() != null ? item.getVariant().getSku() : null;
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                            .productId(productId)
                            .productSlug(productSlug)
                            .productName(item.getProductName())
                            .variantName(item.getVariantName())
                            .imageUrl(productId == null ? null : imageByProductId.get(productId))
                            .sku(sku)
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .subtotal(item.getSubtotal())
                            .build();
                })
                .collect(Collectors.toList());

        User user = order.getUser();
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .customerName(user != null ? user.getFullName() : null)
                .customerEmail(user != null ? user.getEmail() : null)
                .customerPhone(user != null ? user.getPhoneNumber() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }

    String resolveOrderItemImageUrl(OrderItem item) {
        if (item == null || item.getVariant() == null) {
            return null;
        }

        if (item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty()) {
            return item.getVariant().getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(item.getVariant().getImages().get(0).getImageUrl());
        }

        if (item.getVariant().getProduct() != null
                && item.getVariant().getProduct().getImages() != null
                && !item.getVariant().getProduct().getImages().isEmpty()) {
            return item.getVariant().getProduct().getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(item.getVariant().getProduct().getImages().get(0).getImageUrl());
        }

        return null;
    }

    OrderResponse buildCheckoutResponse(Order order, String ipAddress) {
        OrderResponse response = mapToResponse(order);
        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            String url = vnpayPaymentService.createPaymentUrl(order, ipAddress);
            if (url != null) {
                response.setPaymentUrl(url);
            } else {
                response.setPaymentUrl("https://payment.hozitech.com/pay/vnpay/" + order.getOrderNumber());
            }
        } else if (order.getPaymentMethod() != PaymentMethod.COD) {
            response.setPaymentUrl("https://payment.hozitech.com/pay/" + order.getOrderNumber());
        }
        return response;
    }

    String buildTaxLabel(Order order) {
        BigDecimal percent = nz(order.getTaxPercent()).stripTrailingZeros();
        String percentText = percent.toPlainString();
        if (order.getTaxMode() == TaxMode.EXCLUDED) {
            return "Thuế VAT (" + percentText + "%)";
        }
        return "Thuế VAT (" + percentText + "%, đã gồm)";
    }

    @SuppressWarnings("unchecked")
    private String formatShippingAddress(String addressJson) {
        if (addressJson == null || addressJson.isBlank()) {
            return "";
        }
        try {
            var map = objectMapper.readValue(addressJson, java.util.Map.class);
            String fullName = (String) map.getOrDefault("fullName", "");
            String phone = (String) map.getOrDefault("phoneNumber", "");
            String detail = (String) map.getOrDefault("detailAddress", "");
            String ward = (String) map.getOrDefault("ward", "");
            String district = (String) map.getOrDefault("district", "");
            String province = (String) map.getOrDefault("province", "");

            StringBuilder sb = new StringBuilder();
            if (!fullName.isEmpty()) {
                sb.append(fullName);
            }
            if (!phone.isEmpty()) {
                sb.append(" - ").append(phone);
            }
            if (!detail.isEmpty()) {
                sb.append(", ").append(detail);
            }
            if (!ward.isEmpty()) {
                sb.append(", ").append(ward);
            }
            if (!district.isEmpty()) {
                sb.append(", ").append(district);
            }
            if (!province.isEmpty()) {
                sb.append(", ").append(province);
            }
            return sb.toString();
        } catch (Exception e) {
            return addressJson;
        }
    }
}
