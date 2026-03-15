package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.FeedbackService;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleUser;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-client}/feedbacks")
@RoleUser
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> getProductFeedbacks(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch product feedbacks successfully",
                feedbackService.getFeedbacksByProduct(productId, page, size)));
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

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success("Check review status success",
                feedbackService.hasUserReviewedProduct(userDetails.getUser().getId(), productId)));
    }
}
