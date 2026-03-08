package com.hoz.hozitech.application.specifications;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderSpecification {

    public static Specification<Order> filter(
            UUID userId,
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(root.get("phoneNumber"), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
