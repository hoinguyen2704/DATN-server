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

    @NotNull(message = "Product ID is required")
    private UUID productId;

    // Optional: linking feedback directly to the order item purchased
    private UUID orderId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    private String content;

    private String imagesJson; // list of image URLs serialized as JSON array
}
