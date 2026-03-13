package com.hoz.hozitech.application.specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.data.jpa.domain.Specification;

import com.hoz.hozitech.domain.entities.Role_;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.User_;
import com.hoz.hozitech.domain.enums.RoleType;

import jakarta.persistence.criteria.Predicate;

public class UserSpecification {

    public static Specification<User> hasFullNameOrEmail(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get(User_.fullName)), pattern),
                    cb.like(cb.lower(root.get(User_.userName)), pattern),
                    cb.like(cb.lower(root.get(User_.email)), pattern),
                    cb.like(root.get(User_.phoneNumber), pattern));
        };
    }

    public static Specification<User> hasRoleType(RoleType roleType) {
        return (root, query, cb) -> {
            if (roleType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get(User_.role).get(Role_.id), roleType);
        };
    }

    public static Specification<User> filter(
            String keyword,
            RoleType roleType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String status) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // optionally filter by active only if no status provided
                predicates.add(cb.notEqual(root.get("status"), "LOCKED"));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(User_.fullName)), pattern),
                        cb.like(cb.lower(root.get(User_.userName)), pattern),
                        cb.like(cb.lower(root.get(User_.email)), pattern),
                        cb.like(root.get(User_.phoneNumber), pattern)));
            }

            if (roleType != null) {
                predicates.add(cb.equal(root.get(User_.role).get(Role_.id), roleType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(User_.createdAt), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(User_.createdAt), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
