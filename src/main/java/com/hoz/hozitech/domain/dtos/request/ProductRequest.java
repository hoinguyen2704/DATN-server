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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Brand ID is required")
    private UUID brandId;

    @NotNull(message = "Origin price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Origin price must be greater than or equal to 0")
    private BigDecimal originPrice;

    private String specsJson;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private String status;

    private Boolean isFeatured;

    @Valid
    private List<ProductVariantRequest> variants;

    @Valid
    private List<ProductImageRequest> images;
}
