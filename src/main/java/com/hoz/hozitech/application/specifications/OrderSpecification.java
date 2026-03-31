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
                predicates.add(cb.equal(root.get("orderStatus"), status));
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
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(cb.function("text", String.class, root.get("shippingAddressJson"))), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter for order export with user eager-fetch.
     * Searches keyword on orderNumber, user fullName, and user email.
     */
    public static Specification<Order> filterForExport(
            String status,
            String keyword,
            LocalDateTime from,
            LocalDateTime to) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            // Eager fetch user to avoid N+1
            root.fetch("user", jakarta.persistence.criteria.JoinType.LEFT);

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("orderStatus"),
                        OrderStatus.valueOf(status.toUpperCase())));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(root.join("user").get("fullName")), pattern),
                        cb.like(cb.lower(root.join("user").get("email")), pattern)));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(Order_.createdAt), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(Order_.createdAt), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
