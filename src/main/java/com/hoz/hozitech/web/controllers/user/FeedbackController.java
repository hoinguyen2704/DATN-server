package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.feedback.FeedbackService;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.ProductFeedbackPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestAPI("${api.prefix-client}/feedbacks")
@Authenticated
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/product/{productSlug}")
    public ResponseEntity<ApiResponse<ProductFeedbackPageResponse>> getProductFeedbacks(
            @PathVariable String productSlug,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch product feedbacks successfully",
                feedbackService.getFeedbacksByProduct(productSlug, rating, hasComment, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully",
                feedbackService.submitFeedback(userDetails.getUser().getId(), request)));
    }

    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID feedbackId) {
        feedbackService.deleteFeedback(userDetails.getUser().getId(), feedbackId);
        return ResponseEntity.ok(ApiResponse.success("Feedback deleted successfully"));
    }

    @GetMapping("/check/{productSlug}")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(ApiResponse.success("Check review status success",
                feedbackService.hasUserReviewedProduct(userDetails.getUser().getId(), productSlug)));
    }

    @GetMapping("/my-feedback")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String productSlug,
            @RequestParam(required = false) String variantSku,
            @RequestParam(required = false) String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Fetch my feedback success",
                feedbackService.getMyFeedbacks(userDetails.getUser().getId(), productSlug, variantSku, orderNumber)));
    }
}
