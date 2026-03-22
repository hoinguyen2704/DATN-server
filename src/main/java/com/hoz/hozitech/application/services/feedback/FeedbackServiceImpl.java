package com.hoz.hozitech.application.services.feedback;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.feedback.FeedbackService;
import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public PageResponse<FeedbackResponse> getFeedbacksByProduct(UUID productId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        // For public view, only show APPROVED feedbacks
        Page<Feedback> feedbacks = feedbackRepository.findByProductIdAndStatus(productId, "APPROVED", pageable);
        return PageResponse.of(feedbacks.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public FeedbackResponse submitFeedback(UUID userId, FeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElse(null);
            
            // Check if user actually ordered this
            if (order != null && !order.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Order does not belong to user");
            }
        }

        Feedback feedback = Feedback.builder()
                .rating(request.getRating())
                .content(request.getContent())
                .imagesJson(request.getImagesJson())
                .status("APPROVED") // Auto-approve for now, or could default to PENDING
                .user(user)
                .product(product)
                .order(order)
                .build();

        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    public PageResponse<FeedbackResponse> getAllFeedbacks(String status, UUID productId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Feedback> feedbacks;
        if (productId != null && status != null && !status.isBlank()) {
            feedbacks = feedbackRepository.findByProductIdAndStatus(productId, status.toUpperCase(), pageable);
        } else if (productId != null) {
            feedbacks = feedbackRepository.findByProductId(productId, pageable);
        } else if (status != null && !status.isBlank()) {
            feedbacks = feedbackRepository.findByStatus(status.toUpperCase(), pageable);
        } else {
            feedbacks = feedbackRepository.findAll(pageable);
        }

        return PageResponse.of(feedbacks.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedbackStatus(UUID id, String status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        feedback.setStatus(status.toUpperCase()); // APPROVED, HIDDEN, SPAM
        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional
    public void deleteFeedback(UUID userId, UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));
        if (!feedback.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own feedback");
        }
        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(UUID userId, UUID productId) {
        return feedbackRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    public FeedbackResponse adminReplyFeedback(UUID feedbackId, String replyContent) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));
        feedback.setAdminReply(replyContent);
        feedback.setRepliedAt(java.time.LocalDateTime.now());
        return mapToResponse(feedbackRepository.save(feedback));
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .content(feedback.getContent())
                .imagesJson(feedback.getImagesJson())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .productId(feedback.getProduct().getId())
                .productName(feedback.getProduct().getName())
                .userId(feedback.getUser().getId())
                .userName(feedback.getUser().getFullName() != null ? feedback.getUser().getFullName() : feedback.getUser().getUserName())
                .userAvatar(feedback.getUser().getAvatarUrl())
                .orderId(feedback.getOrder() != null ? feedback.getOrder().getId() : null)
                .adminReply(feedback.getAdminReply())
                .repliedAt(feedback.getRepliedAt())
                .build();
    }
}
