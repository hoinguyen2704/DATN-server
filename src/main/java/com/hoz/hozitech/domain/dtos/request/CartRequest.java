package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartRequest {

    @NotNull(message = "{validation.variant_id_is_required}")
    private UUID variantId;

    @NotNull(message = "{validation.quantity_is_required}")
    @Min(value = 1, message = "{validation.quantity_must_be_at_least_1}")
    private Integer quantity;
}
