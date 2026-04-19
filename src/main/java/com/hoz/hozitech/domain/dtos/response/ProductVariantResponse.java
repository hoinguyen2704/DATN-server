package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantResponse {
    private UUID id;
    private String displayName;
    // Alias for downstream screens not yet renamed.
    private String variantName;
    private String sku;
    private String variantSignature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer stockQuantity;
    private Long grossSoldQty;
    private Long returnedQty;
    private Long netSoldQty;
    private Boolean active;
    private List<VariantAttributeValueResponse> selections;
    // Backward-compatible alias for legacy clients.
    private List<VariantAttributeValueResponse> attributes;
    private List<ProductImageResponse> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeValueResponse {
        private UUID variantAttributeId;
        private String attributeName;
        private String attributeCode;
        // Backward-compatible aliases for legacy clients.
        private String variantAttributeName;
        private String variantAttributeCode;
        private UUID optionId;
        private String optionLabel;
        private String optionCode;
    }
}
