package com.hoz.hozitech.application.services.feedback;

import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductFeedbackPageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {
    // Public/Client
    ProductFeedbackPageResponse getFeedbacksByProduct(String productSlug, Integer rating, Boolean hasComment, int page, int size);
    
    FeedbackResponse submitFeedback(UUID userId, FeedbackRequest request, List<MultipartFile> files);

    void deleteFeedback(UUID userId, UUID feedbackId);

    boolean hasUserReviewedProduct(UUID userId, String productSlug);

    List<FeedbackResponse> getMyFeedbacks(UUID userId, String productSlug, String variantSku, String orderNumber);
    
    // Admin
    PageResponse<FeedbackResponse> getAllFeedbacks(String status, UUID productId, int page, int size);
    
    FeedbackResponse updateFeedbackStatus(UUID id, String status);

    FeedbackResponse adminReplyFeedback(UUID feedbackId, String replyContent);
}
