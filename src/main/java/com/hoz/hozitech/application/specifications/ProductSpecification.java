package com.hoz.hozitech.application.specifications;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.data.jpa.domain.Specification;

import com.hoz.hozitech.domain.entities.Category_;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.Product_;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

    public static Specification<Product> filter(
            String keyword,
            java.util.UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Boolean active) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            // Default: not deleted
            predicates.add(cb.isFalse(root.get(Product_.deleted)));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(Product_.name)), pattern),
                        cb.like(cb.lower(root.get(Product_.description)), pattern),
                        cb.like(cb.lower(root.get(Product_.brand)), pattern)));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get(Product_.category).get(Category_.id), categoryId));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get(Product_.brand)), brand.toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(Product_.basePrice), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(Product_.basePrice), maxPrice));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get(Product_.active), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
