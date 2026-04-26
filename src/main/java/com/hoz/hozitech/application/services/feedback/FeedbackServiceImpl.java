package com.hoz.hozitech.application.services.feedback;

import com.hoz.hozitech.domain.enums.FeedbackStatus;
import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.domain.dtos.request.FeedbackRequest;
import com.hoz.hozitech.domain.dtos.response.FeedbackFilterSummaryResponse;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductFeedbackPageResponse;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.application.specifications.FeedbackSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final com.hoz.hozitech.application.repositories.ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AdminNotificationService adminNotificationService;

    @Override
    @Transactional(readOnly = true)
    public ProductFeedbackPageResponse getFeedbacksByProduct(UUID productId, Integer rating, Boolean hasComment, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Feedback> feedbacks = feedbackRepository.findPublicByProductWithFilters(
                productId,
                FeedbackStatus.APPROVED,
                rating,
                hasComment,
                pageable);

        return ProductFeedbackPageResponse.of(
                feedbacks.map(this::mapToResponse),
                buildFeedbackSummary(productId));
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
                throw new UnauthorizedException("Order does not belong to user");
            }
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found"));
        }

        List<Feedback> existingFeedbacks = null;
        if (request.getOrderId() != null && request.getVariantId() != null) {
            existingFeedbacks = feedbackRepository.findAllByUserIdAndProductIdAndVariantIdAndOrderIdOrderByCreatedAtAsc(userId, request.getProductId(), request.getVariantId(), request.getOrderId());
        }

        if (existingFeedbacks != null && existingFeedbacks.size() >= 2) {
            throw new InvalidParamException("Bạn đã đạt giới hạn 2 lần đánh giá cho phân loại này");
        }

        Feedback feedback = Feedback.builder()
                .rating(request.getRating())
                .content(request.getContent())
                .imagesJson(request.getImagesJson())
                .status(FeedbackStatus.APPROVED) // Auto-approve for now, or could default to PENDING
                .user(user)
                .product(product)
                .variant(variant)
                .order(order)
                .editCount(0) // Unused now but kept for compatibility
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        adminNotificationService.createShared(AdminNotificationTemplates.feedbackCreated(saved), false);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMyFeedbacks(UUID userId, UUID productId, UUID variantId, UUID orderId) {
        return feedbackRepository.findAllByUserIdAndProductIdAndVariantIdAndOrderIdOrderByCreatedAtAsc(userId, productId, variantId, orderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> getAllFeedbacks(String status, UUID productId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Feedback> feedbacks = feedbackRepository.findAll(FeedbackSpecification.filter(status, productId), pageable);
        return PageResponse.of(feedbacks.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedbackStatus(UUID id, String status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        feedback.setStatus(FeedbackStatus.valueOf(status.toUpperCase())); // APPROVED, HIDDEN, SPAM
        Feedback saved = feedbackRepository.save(feedback);
        adminNotificationService.createShared(AdminNotificationTemplates.feedbackStatusChanged(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFeedback(UUID userId, UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));
        if (!feedback.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own feedback");
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
                .variantId(feedback.getVariant() != null ? feedback.getVariant().getId() : null)
                .variantName(feedback.getVariant() != null ? feedback.getVariant().getVariantName() : null)
                .userId(feedback.getUser().getId())
                .userName(feedback.getUser().getFullName() != null ? feedback.getUser().getFullName() : feedback.getUser().getUserName())
                .userAvatar(feedback.getUser().getAvatarUrl())
                .orderId(feedback.getOrder() != null ? feedback.getOrder().getId() : null)
                .adminReply(feedback.getAdminReply())
                .repliedAt(feedback.getRepliedAt())
                .editCount(feedback.getEditCount())
                .build();
    }

    private FeedbackFilterSummaryResponse buildFeedbackSummary(UUID productId) {
        Map<Integer, Long> ratingCounts = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toMap(rating -> rating, rating -> 0L));

        feedbackRepository.countRatingDistributionByProductIdAndStatus(productId, FeedbackStatus.APPROVED)
                .forEach(row -> {
                    Integer rating = (Integer) row[0];
                    Long count = ((Number) row[1]).longValue();
                    ratingCounts.put(rating, count);
                });

        return FeedbackFilterSummaryResponse.builder()
                .total(feedbackRepository.countByProductIdAndStatus(productId, FeedbackStatus.APPROVED))
                .withContent(feedbackRepository.countWithContentByProductIdAndStatus(productId, FeedbackStatus.APPROVED))
                .ratingCounts(ratingCounts)
                .build();
    }
}
