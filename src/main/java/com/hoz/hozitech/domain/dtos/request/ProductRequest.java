package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.hoz.hozitech.domain.enums.ProductStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "{validation.product_name_is_required}")
    private String name;

    private String description;

    @NotNull(message = "{validation.brand_id_is_required}")
    private UUID brandId;

    @NotNull(message = "{validation.origin_price_is_required}")
    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.origin_price_must_be_greater_than_or_equal_to_0}")
    private BigDecimal originPrice;

    private String productCode;

    @NotNull(message = "{validation.category_id_is_required}")
    private UUID categoryId;

    private ProductStatus status;

    private Boolean isFeatured;

    @Valid
    private List<ProductVariantRequest> variants;

    @Valid
    private List<ProductSpecRequest> specs;

    @Valid
    private List<ProductImageRequest> images;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductSpecRequest {
        @NotNull(message = "{validation.spec_attribute_id_is_required}")
        private UUID specAttributeId;
        @NotBlank(message = "{validation.spec_value_is_required}")
        private String value;
    }
}
