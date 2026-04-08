package com.hoz.hozitech.application.specifications;

import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.UUID;

public class ReturnRequestSpecification {

    private ReturnRequestSpecification() {
    }

    public static Specification<ReturnRequest> filter(UUID userId, ReturnRequestStatus status, String keyword) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                var orderJoin = root.join("order");
                var userJoin = root.join("user");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("returnNumber")), pattern),
                        cb.like(cb.lower(orderJoin.get("orderNumber")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
