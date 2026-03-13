package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Variant name is required")
    private String variantName;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Compare at price must be greater than or equal to 0")
    private BigDecimal compareAtPrice;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private Boolean active;

    private List<ProductImageRequest> images;
}
