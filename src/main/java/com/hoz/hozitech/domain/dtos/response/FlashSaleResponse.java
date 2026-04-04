package com.hoz.hozitech.domain.dtos.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.hoz.hozitech.domain.enums.FlashSaleStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleResponse {

    private String id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private List<FlashSaleItemResponse> items;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlashSaleItemResponse {
        private String id;
        private String productId;
        private String productSlug;
        private String variantId;
        private String productName;
        private String variantName;
        private String imageUrl;
        private BigDecimal originalPrice;
        private BigDecimal flashPrice;
        private int flashStock;
        private int soldCount;
        private int remainingStock;
    }
}
