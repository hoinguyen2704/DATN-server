package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "{validation.product_slug_is_required}")
    private String productSlug;

    private String variantSku;

    private String orderNumber;

    @NotNull(message = "{validation.rating_is_required}")
    @Min(value = 1, message = "{validation.rating_must_be_at_least_1}")
    @Max(value = 5, message = "{validation.rating_must_not_exceed_5}")
    private Integer rating;

    private String content;

    private String imagesJson; // list of image URLs serialized as JSON array
}
