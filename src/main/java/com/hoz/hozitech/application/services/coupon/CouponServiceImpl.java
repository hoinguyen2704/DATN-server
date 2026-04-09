package com.hoz.hozitech.application.services.coupon;

import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.domain.enums.CouponStatus;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.DiscountType;
import com.hoz.hozitech.domain.enums.CouponApplyType;
import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserSavedCouponRepository;
import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.UserSavedCoupon;
import com.hoz.hozitech.application.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;
    private final UserSavedCouponRepository userSavedCouponRepository;
    private final UserRepository userRepository;

    @Value("${app.timezone}")
    private String appTimezone;

    // ═══════════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getAllCoupons(String keyword, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return PageResponse.of(coupons.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        return mapToResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        return mapToResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new ConflictException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(DiscountType.valueOf(request.getDiscountType().toUpperCase()))
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(CouponStatus.ACTIVE)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .applyType(request.getApplyType() != null ? CouponApplyType.valueOf(request.getApplyType().toUpperCase()) : CouponApplyType.ALL)
                .couponCategory(request.getCouponCategory() != null ? CouponCategory.valueOf(request.getCouponCategory().toUpperCase()) : CouponCategory.PRODUCT)
                .build();

        // Link applicable products
        applyProductScope(coupon, request);

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new ConflictException("Coupon code already exists");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(DiscountType.valueOf(request.getDiscountType().toUpperCase()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : coupon.getIsPublic());
        coupon.setApplyType(request.getApplyType() != null ? CouponApplyType.valueOf(request.getApplyType().toUpperCase()) : coupon.getApplyType());
        coupon.setCouponCategory(request.getCouponCategory() != null ? CouponCategory.valueOf(request.getCouponCategory().toUpperCase()) : coupon.getCouponCategory());

        // Re-link applicable products
        applyProductScope(coupon, request);

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse toggleStatus(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (CouponStatus.ACTIVE == coupon.getStatus()) {
            coupon.setStatus(CouponStatus.INACTIVE);
        } else {
            coupon.setStatus(CouponStatus.ACTIVE);
        }

        return mapToResponse(couponRepository.save(coupon));
    }

    // ═══════════════════════════════════════════════════════════════
    // USER - PUBLIC VOUCHERS
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getPublicCoupons(UUID userId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appTimezone));

        // Fetch public + active vouchers (with or without end date)
        List<Coupon> withEndDate = couponRepository.findByIsPublicTrueAndStatusAndEndDateAfter(CouponStatus.ACTIVE, now);
        List<Coupon> withoutEndDate = couponRepository.findByIsPublicTrueAndStatusAndEndDateIsNull(CouponStatus.ACTIVE);

        List<Coupon> all = new ArrayList<>(withEndDate);
        all.addAll(withoutEndDate);

        // Check which ones user has saved
        Set<UUID> savedCouponIds = new HashSet<>();
        if (userId != null) {
            List<UUID> couponIds = all.stream().map(Coupon::getId).collect(Collectors.toList());
            savedCouponIds = userSavedCouponRepository.findByUserIdAndCouponIdIn(userId, couponIds)
                    .stream().map(usc -> usc.getCoupon().getId()).collect(Collectors.toSet());
        }

        Set<UUID> finalSavedIds = savedCouponIds;
        return all.stream()
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit()) // still available
                .map(c -> {
                    CouponResponse resp = mapToResponse(c);
                    resp.setSaved(finalSavedIds.contains(c.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // USER - SAVE / UNSAVE
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void saveCoupon(UUID userId, UUID couponId) {
        if (userSavedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            return; // Already saved
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        validateCouponSavable(coupon);

        UserSavedCoupon saved = UserSavedCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();
        userSavedCouponRepository.save(saved);
    }

    @Override
    @Transactional
    public void unsaveCoupon(UUID userId, UUID couponId) {
        userSavedCouponRepository.deleteByUserIdAndCouponId(userId, couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getMySavedCoupons(UUID userId) {
        return userSavedCouponRepository.findByUserId(userId).stream()
                .map(usc -> {
                    CouponResponse resp = mapToResponse(usc.getCoupon());
                    resp.setSaved(true);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // USER - VALIDATE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CouponResponse validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        validateCouponAvailability(coupon);
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new InvalidParamException("Order does not meet minimum value for coupon. Minimum is: " + coupon.getMinOrderValue());
        }

        return mapToResponse(coupon);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateCouponAvailability(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appTimezone));
        if (CouponStatus.ACTIVE != coupon.getStatus()) {
            throw new InvalidParamException("Coupon is not active");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
            throw new InvalidParamException("Coupon has expired");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new InvalidParamException("Coupon is not valid yet");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new InvalidParamException("Coupon usage limit exceeded");
        }
    }

    private void validateCouponSavable(Coupon coupon) {
        validateCouponAvailability(coupon);
        if (!Boolean.TRUE.equals(coupon.getIsPublic())) {
            throw new InvalidParamException("Coupon is not public and cannot be saved");
        }
    }

    private void applyProductScope(Coupon coupon, CouponRequest request) {
        if (CouponApplyType.SPECIFIC_PRODUCTS.name().equalsIgnoreCase(request.getApplyType())
                && request.getApplicableProductIds() != null
                && !request.getApplicableProductIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(request.getApplicableProductIds());
            coupon.setApplicableProducts(products);
        } else {
            coupon.getApplicableProducts().clear();
        }
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        List<CouponResponse.ApplicableProductInfo> productInfos = new ArrayList<>();
        if (coupon.getApplicableProducts() != null) {
            productInfos = coupon.getApplicableProducts().stream()
                    .map(p -> CouponResponse.ApplicableProductInfo.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .slug(p.getSlug())
                            .mainImageUrl(p.getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                    .map(ProductImage::getImageUrl)
                                    .findFirst()
                                    .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl()))
                            .build())
                    .collect(Collectors.toList());
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .couponCategory(coupon.getCouponCategory())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .status(coupon.getStatus())
                .isPublic(coupon.getIsPublic())
                .applyType(coupon.getApplyType())
                .applicableProducts(productInfos)
                .build();
    }
}
