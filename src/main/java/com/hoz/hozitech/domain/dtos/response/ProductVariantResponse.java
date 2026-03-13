package com.hoz.hozitech.domain.dtos.response;

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
public class ProductVariantResponse {
    private UUID id;
    private String variantName;
    private String color;
    private String sku;
    private String storageCapacity;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<ProductImageResponse> images;
}
