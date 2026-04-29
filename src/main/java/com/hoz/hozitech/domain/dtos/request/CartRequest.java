package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartRequest {

    @NotBlank(message = "{validation.variant_sku_is_required}")
    private String variantSku;

    @NotNull(message = "{validation.quantity_is_required}")
    @Min(value = 1, message = "{validation.quantity_must_be_at_least_1}")
    private Integer quantity;
}
