package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.coupon.CouponService;
import com.hoz.hozitech.application.services.export.ExportService;
import com.hoz.hozitech.application.services.export.ReportDateRange;
import com.hoz.hozitech.application.services.export.ReportRangeMode;
import com.hoz.hozitech.application.services.export.ReportRangeResolver;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestAPI("${api.prefix-admin}/coupons")
@RoleAdmin
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;
    private final ExportService exportService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.list_fetched",
                couponService.getAllCoupons(keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable UUID id) {
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.fetched",
                couponService.getCouponById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.created",
                couponService.createCoupon(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.updated",
                couponService.updateCoupon(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.status_toggled",
                couponService.toggleStatus(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(responseFactory.success("response.admin_coupon.deleted"));
    }

    @GetMapping("/report-export")
    public ResponseEntity<byte[]> exportCouponsReportByRange(
            @RequestParam ReportRangeMode mode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {
        ReportDateRange range = ReportRangeResolver.resolve(mode, fromDate, toDate, month, year);
        byte[] excelBytes = exportService.exportVouchersReportByRange(keyword, range);

        String filename = "vouchers_report_" + range.fileLabel() + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
