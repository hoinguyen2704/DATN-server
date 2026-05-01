package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.feedback.FeedbackService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.ProductFeedbackPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestAPI("${api.prefix-client}/feedbacks")
@Authenticated
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping("/product/{productSlug}")
    public ResponseEntity<ApiResponse<ProductFeedbackPageResponse>> getProductFeedbacks(
            @PathVariable String productSlug,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success(
                "response.feedback.product_feedbacks_fetched",
                feedbackService.getFeedbacksByProduct(productSlug, rating, hasComment, page, size)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.feedback.submitted",
                feedbackService.submitFeedback(userDetails.getUser().getId(), request, List.of())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedbackMultipart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("payload") FeedbackRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(responseFactory.success(
                "response.feedback.submitted",
                feedbackService.submitFeedback(userDetails.getUser().getId(), request, files)));
    }

    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID feedbackId) {
        feedbackService.deleteFeedback(userDetails.getUser().getId(), feedbackId);
        return ResponseEntity.ok(responseFactory.success("response.feedback.deleted"));
    }

    @GetMapping("/check/{productSlug}")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(responseFactory.success(
                "response.feedback.review_status_checked",
                feedbackService.hasUserReviewedProduct(userDetails.getUser().getId(), productSlug)));
    }

    @GetMapping("/my-feedback")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String productSlug,
            @RequestParam(required = false) String variantSku,
            @RequestParam(required = false) String orderNumber) {
        return ResponseEntity.ok(responseFactory.success(
                "response.feedback.my_feedbacks_fetched",
                feedbackService.getMyFeedbacks(userDetails.getUser().getId(), productSlug, variantSku, orderNumber)));
    }
}
