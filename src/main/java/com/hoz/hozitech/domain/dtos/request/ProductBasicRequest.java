package com.hoz.hozitech.domain.dtos.request;

import com.hoz.hozitech.domain.enums.ProductStatus;
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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductBasicRequest {

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
    private List<ProductRequest.ProductSpecRequest> specs;
}
