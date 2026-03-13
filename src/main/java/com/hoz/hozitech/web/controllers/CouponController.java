package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.CouponService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("${api.prefix-client}/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam(defaultValue = "0") BigDecimal orderAmount) {
        return ResponseEntity.ok(ApiResponse.success("Coupon is valid", couponService.validateCoupon(code, orderAmount)));
    }
}
