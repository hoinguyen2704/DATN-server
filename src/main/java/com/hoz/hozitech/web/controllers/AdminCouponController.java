package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.CouponService;
import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-admin}/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch coupons successfully", couponService.getAllCoupons(keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch coupon details successfully", couponService.getCouponById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon created successfully", couponService.createCoupon(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon updated successfully", couponService.updateCoupon(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Coupon status toggled successfully", couponService.toggleStatus(id)));
    }
}
