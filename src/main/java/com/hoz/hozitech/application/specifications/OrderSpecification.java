package com.hoz.hozitech.application.specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.data.jpa.domain.Specification;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.Order_;
import com.hoz.hozitech.domain.entities.User_;
import com.hoz.hozitech.domain.enums.OrderStatus;

import jakarta.persistence.criteria.Predicate;

public class OrderSpecification {

    public static Specification<Order> filter(
            java.util.UUID userId,
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String keyword) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get(Order_.user).get(User_.id), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get(Order_.status), status));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(Order_.createdAt), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(Order_.createdAt), endDate));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(Order_.orderNumber)), pattern),
                        cb.like(cb.lower(root.get(Order_.fullName)), pattern),
                        cb.like(root.get(Order_.phoneNumber), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
