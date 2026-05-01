package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.coupon.CouponService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestAPI("${api.prefix-client}/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam(defaultValue = "0") BigDecimal orderAmount) {
        return ResponseEntity.ok(responseFactory.success("response.coupon.validated",
                couponService.validateCoupon(code, orderAmount)));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponByCode(@PathVariable String code) {
        return ResponseEntity.ok(responseFactory.success("response.coupon.fetched", couponService.getCouponByCode(code)));
    }

    /**
     * Public vouchers - hiển thị trên storefront cho tất cả mọi người.
     * Nếu user đã đăng nhập, truyền thêm userId để check "đã lưu chưa".
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getPublicCoupons() {
        // Không cần auth, truyền userId = null
        return ResponseEntity.ok(responseFactory.success("response.coupon.public_list_fetched",
                couponService.getPublicCoupons(null)));
    }
}
