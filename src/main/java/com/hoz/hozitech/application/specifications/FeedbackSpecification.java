package com.hoz.hozitech.application.specifications;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.enums.FeedbackStatus;

import jakarta.persistence.criteria.Predicate;

public class FeedbackSpecification {

    public static Specification<Feedback> filter(String status, UUID productId) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }

            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), FeedbackStatus.valueOf(status.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
