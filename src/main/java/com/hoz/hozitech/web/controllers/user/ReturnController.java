package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.order.ReturnService;
import com.hoz.hozitech.domain.dtos.request.CreateReturnRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ReturnRequestResponse;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.web.base.RestAPI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestAPI("${api.prefix-client}/returns")
@Authenticated
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createReturnRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReturnRequest request) {
        ReturnRequestResponse response = returnService.createReturnRequest(
                userDetails.getUser().getId(),
                request,
                List.of(),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Return request created", response));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createReturnRequestMultipart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestPart("payload") CreateReturnRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        ReturnRequestResponse response = returnService.createReturnRequest(
                userDetails.getUser().getId(),
                request,
                files,
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Return request created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReturnRequestResponse>>> getMyReturnRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched",
                returnService.getMyReturnRequests(userDetails.getUser().getId(), status, keyword, page, size)));
    }

    @GetMapping("/{returnNumber}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getReturnByNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success("Return request fetched",
                returnService.getReturnByNumberForUser(userDetails.getUser().getId(), returnNumber)));
    }

    @PatchMapping("/{returnNumber}/cancel")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> cancelReturnRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success("Return request cancelled",
                returnService.cancelReturnRequest(userDetails.getUser().getId(), returnNumber)));
    }
}
