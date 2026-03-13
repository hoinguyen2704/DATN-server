package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "Address ID is required")
    private UUID addressId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // COD, VNPAY, MOMO, BANK_TRANSFER

    private String couponCode;

    private String note;

    @NotNull(message = "Items are required")
    private List<CheckoutItem> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckoutItem {

        @NotNull(message = "Variant ID is required")
        private UUID variantId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
