package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.trimToNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.CouponApplyType;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.CouponStatus;
import com.hoz.hozitech.domain.enums.DiscountType;
import com.hoz.hozitech.web.exceptions.BusinessException;

import lombok.RequiredArgsConstructor;

//  Validates and applies coupons (product & shipping) during checkout.
//  Also handles rolling back coupon usage when an order is cancelled.

@Component
@RequiredArgsConstructor
public class CouponApplier {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    /**
     * Applies a product coupon and returns the discount amount.
     * Returns BigDecimal.ZERO if no coupon code is provided.
     */
    BigDecimal applyProductCoupon(String couponCode, BigDecimal subtotal, Set<UUID> productIds, UUID userId) {
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeForUpdate(couponCode)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.INVALID_PRODUCT_COUPON, "Invalid product coupon code"));

        if (coupon.getCouponCategory() != CouponCategory.PRODUCT) {
            throw new BusinessException(BusinessErrorCode.INVALID_PRODUCT_COUPON, "Voucher is not a product voucher");
        }
        validateCoupon(coupon, subtotal, productIds, userId);

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (coupon.getMaxDiscountAmount() != null && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discountAmount = coupon.getMaxDiscountAmount();
            }
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return discountAmount;
    }

    /**
     * Applies a shipping coupon and returns the shipping discount amount.
     * Returns BigDecimal.ZERO if no coupon code is provided.
     */
    BigDecimal applyShippingCoupon(String shippingCouponCode, BigDecimal shippingFee, BigDecimal subtotal, Set<UUID> productIds, UUID userId) {
        if (shippingCouponCode == null || shippingCouponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeForUpdate(shippingCouponCode)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.INVALID_SHIPPING_COUPON, "Invalid shipping coupon code"));

        if (coupon.getCouponCategory() != CouponCategory.SHIPPING) {
            throw new BusinessException(BusinessErrorCode.INVALID_SHIPPING_COUPON, "Voucher is not a freeship voucher");
        }
        validateCoupon(coupon, subtotal, productIds, userId);

        BigDecimal shippingDiscountAmount;
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

        return shippingDiscountAmount;
    }

    /**
     * Restores coupon usage counts when an order is cancelled.
     */
    public void restoreCouponUsage(String productCouponCode, String shippingCouponCode) {
        Set<String> couponCodesToRestore = new LinkedHashSet<>();
        String normalizedProduct = trimToNull(productCouponCode);
        String normalizedShipping = trimToNull(shippingCouponCode);
        if (normalizedProduct != null) couponCodesToRestore.add(normalizedProduct);
        if (normalizedShipping != null) couponCodesToRestore.add(normalizedShipping);

        for (String code : couponCodesToRestore) {
            couponRepository.findByCodeForUpdate(code).ifPresent(coupon -> {
                int currentUsedCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
                if (currentUsedCount > 0) {
                    coupon.setUsedCount(currentUsedCount - 1);
                    couponRepository.save(coupon);
                }
            });
        }
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal, Set<UUID> checkedOutProductIds, UUID userId) {
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
        if (userId != null && orderRepository.existsCouponUsedByUser(userId, coupon.getCode())) {
            throw new BusinessException(
                    BusinessErrorCode.COUPON_ALREADY_USED_BY_USER,
                    "Mỗi khách hàng chỉ được sử dụng mã giảm giá này một lần");
        }
        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BusinessException(BusinessErrorCode.COUPON_MIN_ORDER_NOT_MET, "Order does not meet minimum value for coupon");
        }
        if (coupon.getApplyType() == CouponApplyType.SPECIFIC_PRODUCTS) {
            boolean applicable = coupon.getApplicableProducts() != null
                    && checkedOutProductIds != null
                    && !checkedOutProductIds.isEmpty()
                    && coupon.getApplicableProducts().stream()
                    .map(product -> product.getId())
                    .anyMatch(checkedOutProductIds::contains);
            if (!applicable) {
                throw new BusinessException(
                        BusinessErrorCode.COUPON_NOT_APPLICABLE_TO_ITEMS,
                        "Coupon is not applicable to selected products");
            }
        }
    }
}
