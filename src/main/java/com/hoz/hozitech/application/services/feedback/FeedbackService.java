package com.hoz.hozitech.application.services.feedback;

import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductFeedbackPageResponse;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {
    // Public/Client
    ProductFeedbackPageResponse getFeedbacksByProduct(UUID productId, Integer rating, Boolean hasComment, int page, int size);
    
    FeedbackResponse submitFeedback(UUID userId, FeedbackRequest request);

    void deleteFeedback(UUID userId, UUID feedbackId);

    boolean hasUserReviewedProduct(UUID userId, UUID productId);

    List<FeedbackResponse> getMyFeedbacks(UUID userId, UUID productId, UUID variantId, UUID orderId);
    
    // Admin
    PageResponse<FeedbackResponse> getAllFeedbacks(String status, UUID productId, int page, int size);
    
    FeedbackResponse updateFeedbackStatus(UUID id, String status);

    FeedbackResponse adminReplyFeedback(UUID feedbackId, String replyContent);
}
