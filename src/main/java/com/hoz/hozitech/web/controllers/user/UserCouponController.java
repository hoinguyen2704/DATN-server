package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.coupon.CouponService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.web.base.RestAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.hoz.hozitech.web.base.Authenticated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestAPI("${api.prefix-client}/my-coupons")
@Authenticated
@RequiredArgsConstructor
public class UserCouponController {

    private final CouponService couponService;

    /**
     * Danh sách voucher công khai (có đánh dấu "đã lưu" theo user).
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getPublicCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Public coupons",
                couponService.getPublicCoupons(userDetails.getUser().getId())));
    }

    /**
     * Danh sách voucher đã lưu của user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getMySavedCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Saved coupons",
                couponService.getMySavedCoupons(userDetails.getUser().getId())));
    }

    /**
     * Lưu voucher vào ví.
     */
    @PostMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> saveCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID couponId) {
        couponService.saveCoupon(userDetails.getUser().getId(), couponId);
        return ResponseEntity.ok(ApiResponse.success("Voucher saved successfully"));
    }

    /**
     * Bỏ lưu voucher.
     */
    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> unsaveCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID couponId) {
        couponService.unsaveCoupon(userDetails.getUser().getId(), couponId);
        return ResponseEntity.ok(ApiResponse.success("Voucher removed from saved"));
    }
}
