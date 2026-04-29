package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantsUpdateRequest {

    @Valid
    @NotEmpty(message = "{validation.at_least_one_variant_is_required}")
    private List<ProductVariantRequest> variants;
}
