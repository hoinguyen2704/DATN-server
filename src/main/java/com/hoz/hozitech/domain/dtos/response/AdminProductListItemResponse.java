package com.hoz.hozitech.domain.dtos.response;

import com.hoz.hozitech.domain.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductListItemResponse {
    private UUID id;
    private String name;
    private String slug;
    private UUID brandId;
    private String brandName;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String productCode;
    private BigDecimal originPrice;
    private BigDecimal lowestPrice;
    private ProductStatus status;
    private Boolean isFeatured;
    private Integer totalSold;
    private Integer totalStock;
    private Boolean outOfStock;
    private String mainImageUrl;
    private LocalDateTime createdAt;
}
