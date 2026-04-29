package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantRequest {

    private UUID id;

    private String sku;

    @NotNull(message = "{validation.price_is_required}")
    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.price_must_be_greater_than_or_equal_to_0}")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.compare_at_price_must_be_greater_than_or_equal_to_0}")
    private BigDecimal compareAtPrice;

    @Min(value = 0, message = "{validation.stock_cannot_be_negative}")
    private Integer stock;

    private Boolean active;

    private List<VariantSelectionRequest> selections;

    private List<ProductImageRequest> images;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantSelectionRequest {
        @NotNull(message = "{validation.variant_attribute_id_is_required}")
        private UUID variantAttributeId;
        @NotNull(message = "{validation.option_id_is_required}")
        private UUID optionId;
    }
}
