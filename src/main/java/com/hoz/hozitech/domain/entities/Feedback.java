package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "feedbacks", indexes = {
        @Index(name = "idx_feedback_product", columnList = "product_id"),
        @Index(name = "idx_feedback_user", columnList = "user_id")
})
public class Feedback extends AbstractAuditingEntity {

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Min(1)
    @Max(5)
    @Column(name = "star", nullable = false)
    private Integer star;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
