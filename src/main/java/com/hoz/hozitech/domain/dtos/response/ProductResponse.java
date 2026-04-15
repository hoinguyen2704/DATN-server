package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.hoz.hozitech.domain.enums.ProductStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private UUID brandId;
    private String brandName;
    private CategoryResponse category; // Brief info or full category hierarchy depending on mapping
    private String productCode;
    private BigDecimal originPrice;
    private BigDecimal lowestPrice;
    private Double averageRating;
    private Integer totalReviews;
    private ProductStatus status;
    private Boolean isFeatured;
    private List<SpecSchemaResponse> specSchema;
    private List<ProductSpecValueResponse> specs;
    private Integer totalSold;
    private LocalDateTime createdAt;

    // Derived properties
    private String mainImageUrl; // Convenient field for listing pages
    private Boolean outOfStock;

    // Complex mapping
    private List<ProductImageResponse> images;
    private List<VariantAttributeSchemaResponse> variantSchema;
    private List<ProductVariantResponse> variants;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductSpecValueResponse {
        private UUID specAttributeId;
        private String name;
        private String code;
        // Backward-compatible alias for legacy clients.
        private String specCode;
        private String value;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpecSchemaResponse {
        private UUID id;
        private String name;
        private String code;
        private String hint;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantAttributeSchemaResponse {
        private UUID id;
        private String name;
        private String code;
        private Integer sortOrder;
        private List<VariantOptionResponse> options;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantOptionResponse {
        private UUID id;
        private String label;
        private String code;
        private Integer sortOrder;
        private Boolean active;
    }
}
