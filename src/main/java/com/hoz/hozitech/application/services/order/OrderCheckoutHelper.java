package com.hoz.hozitech.application.services.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.services.flashsale.FlashSaleService;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.domain.dtos.request.CheckoutRequest;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.ProductStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


// Handles the construction phase of checkout: building order items,
// validating variants, managing stock, and generating order numbers.

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCheckoutHelper {

    private final ProductVariantRepository variantRepository;
    private final FlashSaleService flashSaleService;
    private final SettingService settingService;
    private final ObjectMapper objectMapper;

    /**
     * Builds order items from checkout request, validates each variant,
     * applies flash sale pricing, and reduces stock.
     */
    CheckoutItemsResult buildOrderItems(List<CheckoutRequest.CheckoutItem> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Set<UUID> checkedOutVariantIds = new LinkedHashSet<>();
        Set<UUID> checkedOutProductIds = new LinkedHashSet<>();

        for (CheckoutRequest.CheckoutItem item : items) {
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getVariantId())
                    .orElseThrow(() -> new BusinessException(
                            BusinessErrorCode.VARIANT_NOT_FOUND,
                            "Product variant not found: " + item.getVariantId())
                            .withMessageKey("error.variant_not_found", item.getVariantId()));
            validateVariantPurchasable(variant);

            if (variant.getStock() < item.getQuantity()) {
                throw new BusinessException(
                        BusinessErrorCode.INSUFFICIENT_STOCK,
                        "Not enough stock for: " + variant.getVariantName())
                        .withMessageKey("error.insufficient_stock_for_variant", variant.getVariantName());
            }

            // Check Flash Sale first
            BigDecimal flashPrice = flashSaleService.applyFlashSaleAndReduceStock(variant.getId(), item.getQuantity());
            BigDecimal unitPrice = (flashPrice != null) ? flashPrice : variant.getPrice();
            if (item.getExpectedUnitPrice() != null && item.getExpectedUnitPrice().compareTo(unitPrice) != 0) {
                throw new BusinessException(
                        BusinessErrorCode.PRICE_CHANGED,
                        "Price has changed for: " + variant.getVariantName() + ". Latest price: " + unitPrice)
                        .withMessageKey("error.price_changed_for_variant", variant.getVariantName(), unitPrice);
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
            if (variant.getProduct() != null && variant.getProduct().getId() != null) {
                checkedOutProductIds.add(variant.getProduct().getId());
            }

            // Reduce base stock
            variant.setStock(variant.getStock() - item.getQuantity());
            variantRepository.save(variant);
        }

        if (orderItems.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.EMPTY_CHECKOUT_ITEMS, "No valid order item found");
        }

        return new CheckoutItemsResult(orderItems, subtotal, checkedOutVariantIds, checkedOutProductIds);
    }

    /**
     * Calculates shipping fee based on system settings and subtotal.
     */
    BigDecimal calculateShippingFee(BigDecimal subtotal) {
        BigDecimal defaultFee = settingService.getSettingNumber("DEFAULT_SHIPPING_FEE");
        BigDecimal threshold = settingService.getSettingNumber("FREE_SHIPPING_THRESHOLD");
        return (threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0)
                ? BigDecimal.ZERO : defaultFee;
    }

    /**
     * Serializes an Address to JSON for order snapshot.
     */
    String snapshotAddress(Address address) {
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

    /**
     * Generates a unique order number in format ORD-yyyyMMdd-XXXXXX.
     */
    String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return "ORD-" + date + "-" + random;
    }

    /**
     * Parses and validates a payment method string.
     */
    PaymentMethod parsePaymentMethod(String paymentMethodRaw) {
        try {
            return PaymentMethod.valueOf(paymentMethodRaw.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_PAYMENT_METHOD, "Invalid payment method: " + paymentMethodRaw)
                    .withMessageKey("error.invalid_payment_method", paymentMethodRaw);
        }
    }

    /**
     * Restores stock and flash sale counts for all items in a cancelled order.
     */
    public void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            if (item.getVariant() == null) continue;
            UUID variantId = item.getVariant().getId();
            ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElse(item.getVariant());
            variant.setStock(variant.getStock() + item.getQuantity());
            variantRepository.save(variant);

            flashSaleService.restoreFlashSaleSoldCount(
                    variantId,
                    item.getUnitPrice(),
                    item.getQuantity(),
                    order.getCreatedAt());
        }
    }

    private void validateVariantPurchasable(ProductVariant variant) {
        if (variant.getProduct() == null || variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_AVAILABLE, "Product is not available for purchase");
        }
        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new BusinessException(BusinessErrorCode.VARIANT_NOT_AVAILABLE, "Product variant is not available for purchase");
        }
        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_AVAILABLE, "Product price is invalid or requires contact");
        }
    }

    /**
     * Holds the result of building order items during checkout.
     */
    @Getter
    @RequiredArgsConstructor
    static class CheckoutItemsResult {
        private final List<OrderItem> orderItems;
        private final BigDecimal subtotal;
        private final Set<UUID> checkedOutVariantIds;
        private final Set<UUID> checkedOutProductIds;
    }
}
