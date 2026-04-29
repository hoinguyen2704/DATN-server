package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Max;
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
public class FeedbackRequest {

    @NotNull(message = "{validation.product_id_is_required}")
    private UUID productId;

    private UUID variantId;

    // Optional: linking feedback directly to the order item purchased
    private UUID orderId;

    @NotNull(message = "{validation.rating_is_required}")
    @Min(value = 1, message = "{validation.rating_must_be_at_least_1}")
    @Max(value = 5, message = "{validation.rating_must_not_exceed_5}")
    private Integer rating;

    private String content;

    private String imagesJson; // list of image URLs serialized as JSON array
}
