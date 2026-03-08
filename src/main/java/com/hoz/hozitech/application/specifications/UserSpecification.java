package com.hoz.hozitech.application.specifications;

import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.RoleType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filter(
            String keyword,
            RoleType roleType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean deleted) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (deleted != null) {
                predicates.add(cb.equal(root.get("deleted"), deleted));
            } else {
                predicates.add(cb.isFalse(root.get("deleted")));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("userName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(root.get("phoneNumber"), pattern)));
            }

            if (roleType != null) {
                predicates.add(cb.equal(root.get("role").get("id"), roleType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
