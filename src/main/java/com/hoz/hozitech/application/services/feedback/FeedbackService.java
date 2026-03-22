package com.hoz.hozitech.application.services.feedback;

import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface FeedbackService {
    // Public/Client
    PageResponse<FeedbackResponse> getFeedbacksByProduct(UUID productId, int page, int size);
    
    FeedbackResponse submitFeedback(UUID userId, FeedbackRequest request);

    void deleteFeedback(UUID userId, UUID feedbackId);

    boolean hasUserReviewedProduct(UUID userId, UUID productId);
    
    // Admin
    PageResponse<FeedbackResponse> getAllFeedbacks(String status, UUID productId, int page, int size);
    
    FeedbackResponse updateFeedbackStatus(UUID id, String status);

    FeedbackResponse adminReplyFeedback(UUID feedbackId, String replyContent);
}
