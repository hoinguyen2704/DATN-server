package com.hoz.hozitech.application.services.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.repositories.UserSavedCouponRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.promotion.PromotionStatusSyncService;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.NotFoundException;
import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.UserSavedCoupon;
import com.hoz.hozitech.domain.enums.CouponApplyType;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.CouponStatus;
import com.hoz.hozitech.domain.enums.DiscountType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String COUPON_NOT_FOUND_MESSAGE = "Coupon not found";
    private static final String COUPON_NOT_FOUND_MESSAGE_KEY = "error.literal.coupon_not_found";

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserSavedCouponRepository userSavedCouponRepository;
    private final UserRepository userRepository;
    private final AdminNotificationService adminNotificationService;
    private final PromotionStatusSyncService promotionStatusSyncService;

    @Value("${app.timezone}")
    private String appTimezone;

    //
    // ADMIN
    //

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getAllCoupons(String keyword, int page, int size) {
        promotionStatusSyncService.syncCouponStatuses();
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Coupon> coupons = keyword != null && !keyword.isBlank()
                ? couponRepository.findByCodeContainingIgnoreCase(keyword.trim(), pageable)
                : couponRepository.findAll(pageable);
        Map<UUID, List<UUID>> applicableProductIdsByCouponId = buildApplicableProductIdsByCouponId(coupons.getContent().stream()
                .map(Coupon::getId)
                .toList());
        return PageResponse.of(coupons.map(coupon -> mapToAdminListResponse(
                coupon,
                applicableProductIdsByCouponId.getOrDefault(coupon.getId(), List.of()))));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(UUID id) {
        promotionStatusSyncService.syncCouponStatuses();
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(this::couponNotFound);
        return mapToResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        String normalizedCode = normalizeCouponCode(code);
        if (normalizedCode == null) {
            throw couponNotFound();
        }
        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(this::couponNotFound);
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
                .status(resolveCouponStatus(CouponStatus.ACTIVE, request.getEndDate()))
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .applyType(
                        request.getApplyType() != null ? CouponApplyType.valueOf(request.getApplyType().toUpperCase())
                                : CouponApplyType.ALL)
                .couponCategory(request.getCouponCategory() != null
                        ? CouponCategory.valueOf(request.getCouponCategory().toUpperCase())
                        : CouponCategory.PRODUCT)
                .build();

        // Link applicable products
        applyProductScope(coupon, request);

        Coupon saved = couponRepository.save(coupon);
        adminNotificationService.createShared(AdminNotificationTemplates.couponCreated(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(this::couponNotFound);

        if (!coupon.getCode().equalsIgnoreCase(request.getCode())
                && orderRepository.existsByCouponCodeInAnyOrder(coupon.getCode())) {
            throw new ConflictException(
                    "Mã voucher này đã phát sinh đơn hàng, không thể thay đổi. Vui lòng tạo voucher mới nếu cần dùng mã khác!");
        }

        if (!coupon.getCode().equalsIgnoreCase(request.getCode())
                && couponRepository.existsByCode(request.getCode().toUpperCase())) {
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
        coupon.setApplyType(
                request.getApplyType() != null ? CouponApplyType.valueOf(request.getApplyType().toUpperCase())
                        : coupon.getApplyType());
        coupon.setCouponCategory(
                request.getCouponCategory() != null ? CouponCategory.valueOf(request.getCouponCategory().toUpperCase())
                        : coupon.getCouponCategory());
        coupon.setStatus(resolveCouponStatus(coupon.getStatus(), coupon.getEndDate()));

        // Re-link applicable products
        applyProductScope(coupon, request);

        Coupon saved = couponRepository.save(coupon);
        adminNotificationService.createShared(AdminNotificationTemplates.couponUpdated(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CouponResponse toggleStatus(UUID id) {
        promotionStatusSyncService.syncCouponStatuses();
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(this::couponNotFound);

        if (coupon.getStatus() == CouponStatus.EXPIRED) {
            return mapToResponse(coupon);
        }

        if (CouponStatus.ACTIVE == coupon.getStatus()) {
            coupon.setStatus(CouponStatus.INACTIVE);
        } else {
            coupon.setStatus(CouponStatus.ACTIVE);
        }

        Coupon saved = couponRepository.save(coupon);
        adminNotificationService.createShared(AdminNotificationTemplates.couponStatusChanged(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(this::couponNotFound);

        if (orderRepository.existsByCouponCodeInAnyOrder(coupon.getCode())) {
            throw new ConflictException(
                    "Voucher này đã phát sinh đơn hàng, không thể xoá cứng. Vui lòng chuyển trạng thái sang Inactive!");
        }

        coupon.getApplicableProducts().clear();
        couponRepository.saveAndFlush(coupon);
        userSavedCouponRepository.deleteByCouponId(id);
        couponRepository.delete(coupon);
    }

    //
    // USER - PUBLIC VOUCHERS
    //

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getPublicCoupons(UUID userId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appTimezone));
        List<Coupon> all = couponRepository.findVisiblePublicCoupons(CouponStatus.ACTIVE, now);

        // Check which ones user has saved
        Set<UUID> savedCouponIds = new HashSet<>();
        if (userId != null) {
            List<UUID> couponIds = all.stream().map(Coupon::getId).collect(Collectors.toList());
            savedCouponIds = userSavedCouponRepository.findByUserIdAndCouponIdIn(userId, couponIds)
                    .stream().map(usc -> usc.getCoupon().getId()).collect(Collectors.toSet());
        }

        Set<UUID> finalSavedIds = savedCouponIds;
        Map<UUID, List<UUID>> applicableProductIdsByCouponId = buildApplicableProductIdsByCouponId(all.stream()
                .map(Coupon::getId)
                .toList());
        return all.stream()
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit()) // still available
                .map(c -> {
                    CouponResponse resp = mapToResponse(
                            c,
                            applicableProductIdsByCouponId.getOrDefault(c.getId(), List.of()));
                    resp.setSaved(finalSavedIds.contains(c.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    //
    // USER - SAVE / UNSAVE
    //

    @Override
    @Transactional
    public void saveCoupon(UUID userId, String code) {
        String normalizedCode = normalizeCouponCode(code);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (normalizedCode == null) {
            throw couponNotFound();
        }
        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(this::couponNotFound);
        if (userSavedCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())) {
            return;
        }
        validateCouponSavable(coupon);

        UserSavedCoupon saved = UserSavedCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();
        userSavedCouponRepository.save(saved);
    }

    @Override
    @Transactional
    public void unsaveCoupon(UUID userId, String code) {
        String normalizedCode = normalizeCouponCode(code);
        if (normalizedCode == null) {
            throw couponNotFound();
        }
        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(this::couponNotFound);
        userSavedCouponRepository.deleteByUserIdAndCouponId(userId, coupon.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getMySavedCoupons(UUID userId) {
        List<UserSavedCoupon> savedCoupons = userSavedCouponRepository.findByUserId(userId);
        Map<UUID, List<UUID>> applicableProductIdsByCouponId = buildApplicableProductIdsByCouponId(savedCoupons.stream()
                .map(usc -> usc.getCoupon().getId())
                .toList());
        return savedCoupons.stream()
                .map(usc -> {
                    CouponResponse resp = mapToResponse(
                            usc.getCoupon(),
                            applicableProductIdsByCouponId.getOrDefault(usc.getCoupon().getId(), List.of()));
                    resp.setSaved(true);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    //
    // USER - VALIDATE
    //

    @Override
    public CouponResponse validateCoupon(String code, BigDecimal orderAmount) {
        String normalizedCode = normalizeCouponCode(code);
        if (normalizedCode == null) {
            throw invalidCouponCode();
        }
        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(this::invalidCouponCode);

        validateCouponAvailability(coupon);
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new InvalidParamException(
                    "Order does not meet minimum value for coupon. Minimum is: " + coupon.getMinOrderValue())
                    .withMessageKey("error.coupon_min_order_not_met_with_minimum", coupon.getMinOrderValue());
        }

        return mapToResponse(coupon);
    }

    //
    // PRIVATE HELPERS
    //

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

    private CouponStatus resolveCouponStatus(CouponStatus currentStatus, LocalDateTime endDate) {
        if (endDate != null && endDate.isBefore(LocalDateTime.now(ZoneId.of(appTimezone)))) {
            return CouponStatus.EXPIRED;
        }
        return currentStatus == CouponStatus.EXPIRED ? CouponStatus.ACTIVE : currentStatus;
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

    private String normalizeCouponCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim().toUpperCase();
        return normalizedCode.isEmpty() ? null : normalizedCode;
    }

    private NotFoundException couponNotFound() {
        return new NotFoundException(COUPON_NOT_FOUND_MESSAGE)
                .withMessageKey(COUPON_NOT_FOUND_MESSAGE_KEY);
    }

    private InvalidParamException invalidCouponCode() {
        return new InvalidParamException("Invalid coupon code")
                .withMessageKey(COUPON_NOT_FOUND_MESSAGE_KEY);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        List<UUID> applicableProductIds = coupon.getApplicableProducts() == null
                ? List.of()
                : coupon.getApplicableProducts().stream()
                        .map(Product::getId)
                        .toList();
        return mapToResponse(coupon, applicableProductIds);
    }

    private CouponResponse mapToResponse(Coupon coupon, List<UUID> applicableProductIds) {
        List<CouponResponse.ApplicableProductInfo> productInfos = applicableProductIds.stream()
                .map(productId -> CouponResponse.ApplicableProductInfo.builder()
                        .id(productId)
                        .build())
                .toList();

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

    private CouponResponse mapToAdminListResponse(Coupon coupon, List<UUID> applicableProductIds) {
        List<CouponResponse.ApplicableProductInfo> productInfos = applicableProductIds.stream()
                .map(productId -> CouponResponse.ApplicableProductInfo.builder()
                        .id(productId)
                        .build())
                .toList();

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

    private Map<UUID, List<UUID>> buildApplicableProductIdsByCouponId(Collection<UUID> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<UUID>> productIdsByCouponId = new LinkedHashMap<>();
        for (Object[] row : couponRepository.findApplicableProductPairsByCouponIds(couponIds)) {
            UUID couponId = (UUID) row[0];
            UUID productId = (UUID) row[1];
            productIdsByCouponId.computeIfAbsent(couponId, ignored -> new ArrayList<>()).add(productId);
        }
        return productIdsByCouponId;
    }
}
