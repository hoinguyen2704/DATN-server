package com.hoz.hozitech.web.controllers.admin;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.export.ExportService;
import com.hoz.hozitech.application.services.export.ReportDateRange;
import com.hoz.hozitech.application.services.export.ReportRangeMode;
import com.hoz.hozitech.application.services.export.ReportRangeResolver;
import com.hoz.hozitech.application.services.order.ReturnService;
import com.hoz.hozitech.domain.dtos.request.ProcessRefundRequest;
import com.hoz.hozitech.domain.dtos.request.ReviewReturnRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateReturnStatusRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ReturnRequestResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestAPI("${api.prefix-admin}/returns")
@RoleAdmin
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;
    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReturnRequestResponse>>> getAllReturnRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_LARGE_STR) int size) {
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched",
                returnService.getAllReturnRequests(status, keyword, page, size)));
    }

    @GetMapping("/{returnNumber}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getReturnByNumber(@PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success("Return request fetched",
                returnService.getReturnByNumberForAdmin(returnNumber)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReturns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        byte[] excelBytes = exportService.exportReturnsToExcel(status, keyword);

        String filename = "returns_" + LocalDate.now() + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/report-export")
    public ResponseEntity<byte[]> exportReturnsReportByRange(
            @RequestParam ReportRangeMode mode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {
        ReportDateRange range = ReportRangeResolver.resolve(mode, fromDate, toDate, month, year);
        byte[] excelBytes = exportService.exportReturnsReportByRange(status, keyword, range);

        String filename = "returns_report_" + range.fileLabel() + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @PatchMapping("/{returnRequestId}/review")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> reviewReturnRequest(
            @PathVariable UUID returnRequestId,
            @Valid @RequestBody ReviewReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return request reviewed",
                returnService.reviewReturnRequest(returnRequestId, request)));
    }

    @PatchMapping("/{returnRequestId}/status")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> updateReturnStatus(
            @PathVariable UUID returnRequestId,
            @Valid @RequestBody UpdateReturnStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return status updated",
                returnService.updateReturnStatus(returnRequestId, request)));
    }

    @PostMapping("/{returnRequestId}/refund")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> processRefund(
            @PathVariable UUID returnRequestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProcessRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed",
                returnService.processRefund(returnRequestId, request, idempotencyKey)));
    }
}
