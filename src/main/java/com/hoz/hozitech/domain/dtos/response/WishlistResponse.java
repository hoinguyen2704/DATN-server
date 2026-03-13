package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productSlug;
    private BigDecimal productPrice;
    private BigDecimal productCompareAtPrice;
    private String productThumbnailUrl;
    private LocalDateTime addedAt;
}
