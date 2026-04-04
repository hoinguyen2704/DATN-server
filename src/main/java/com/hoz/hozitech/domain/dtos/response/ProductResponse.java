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
    private BigDecimal originPrice;
    private BigDecimal lowestPrice;
    private Double averageRating;
    private Integer totalReviews;
    private ProductStatus status;
    private Boolean isFeatured;
    private String specsJson;
    private Integer totalSold;
    private LocalDateTime createdAt;

    // Derived properties
    private String mainImageUrl; // Convenient field for listing pages
    private Boolean outOfStock;

    // Complex mapping
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
}
