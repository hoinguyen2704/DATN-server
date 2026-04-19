package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductVariantSummaryResponse {
    private UUID id;
    private UUID productId;
    private String variantName;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long grossSoldQty;
    private Long returnedQty;
    private Long netSoldQty;
    private Boolean active;
    private String imageUrl;
}
