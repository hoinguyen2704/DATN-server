package com.hoz.hozitech.application.services.coupon;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.constant.StatusConstant;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;
    private final UserSavedCouponRepository userSavedCouponRepository;
    private final UserRepository userRepository;

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
            throw new IllegalArgumentException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType().toUpperCase())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(StatusConstant.COUPON_ACTIVE)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .applyType(request.getApplyType() != null ? request.getApplyType() : StatusConstant.COUPON_APPLY_ALL)
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
            throw new IllegalArgumentException("Coupon code already exists");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(request.getDiscountType().toUpperCase());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : coupon.getIsPublic());
        coupon.setApplyType(request.getApplyType() != null ? request.getApplyType() : coupon.getApplyType());

        // Re-link applicable products
        applyProductScope(coupon, request);

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse toggleStatus(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (StatusConstant.COUPON_ACTIVE.equalsIgnoreCase(coupon.getStatus())) {
            coupon.setStatus(StatusConstant.COUPON_INACTIVE);
        } else {
            coupon.setStatus(StatusConstant.COUPON_ACTIVE);
        }

        return mapToResponse(couponRepository.save(coupon));
    }

    // ═══════════════════════════════════════════════════════════════
    // USER - PUBLIC VOUCHERS
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getPublicCoupons(UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        // Fetch public + active vouchers (with or without end date)
        List<Coupon> withEndDate = couponRepository.findByIsPublicTrueAndStatusAndEndDateAfter(StatusConstant.COUPON_ACTIVE, now);
        List<Coupon> withoutEndDate = couponRepository.findByIsPublicTrueAndStatusAndEndDateIsNull(StatusConstant.COUPON_ACTIVE);

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

        if (!StatusConstant.COUPON_ACTIVE.equalsIgnoreCase(coupon.getStatus())) {
            throw new IllegalArgumentException("Coupon is not active");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Coupon has expired");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Coupon is not valid yet");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("Coupon usage limit exceeded");
        }
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Order does not meet minimum value for coupon. Minimum is: " + coupon.getMinOrderValue());
        }

        return mapToResponse(coupon);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void applyProductScope(Coupon coupon, CouponRequest request) {
        if (StatusConstant.COUPON_APPLY_SPECIFIC.equalsIgnoreCase(request.getApplyType())
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
