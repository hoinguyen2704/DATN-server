package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/returns")
@RoleAdmin
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

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
