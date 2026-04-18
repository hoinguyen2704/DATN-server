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
        return filter(keyword, categoryId, brand, minPrice, maxPrice, inStock, active, null);
    }

    public static Specification<Product> filter(
            String keyword,
            java.util.UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Boolean active,
            String status) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            // Default: not archived
            predicates.add(cb.notEqual(root.get("status"), "ARCHIVED"));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(Product_.name)), pattern),
                        cb.like(cb.lower(root.get(Product_.description)), pattern),
                        cb.like(cb.lower(root.get("brand").get("name")), pattern)));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get(Product_.category).get(Category_.id), categoryId));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand").get("slug")), brand.toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("originPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("originPrice"), maxPrice));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            } else if (active != null) {
                predicates.add(cb.equal(root.get("status"), active ? "ACTIVE" : "DRAFT"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Simplified filter for product export (keyword searches name/slug only).
     */
    public static Specification<Product> filterForExport(
            String keyword,
            java.util.UUID categoryId,
            String status) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(Product_.name)), pattern),
                        cb.like(cb.lower(root.get(Product_.slug)), pattern)));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get(Product_.category).get(Category_.id), categoryId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
