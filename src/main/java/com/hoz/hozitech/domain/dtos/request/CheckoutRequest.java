package com.hoz.hozitech.domain.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "{validation.address_id_is_required}")
    private UUID addressId;

    @NotBlank(message = "{validation.payment_method_is_required}")
    private String paymentMethod; // COD, VNPAY, MOMO, BANK_TRANSFER

    private String couponCode;

    private String shippingCouponCode;

    private String note;

    @NotEmpty(message = "{validation.items_must_not_be_empty}")
    private List<@Valid CheckoutItem> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckoutItem {

        @NotNull(message = "{validation.variant_id_is_required}")
        private UUID variantId;

        @NotNull(message = "{validation.quantity_is_required}")
        @Min(value = 1, message = "{validation.quantity_must_be_at_least_1}")
        private Integer quantity;

        private BigDecimal expectedUnitPrice;
    }
}
