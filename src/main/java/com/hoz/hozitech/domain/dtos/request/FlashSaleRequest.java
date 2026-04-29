package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleRequest {

    @NotBlank(message = "{validation.ten_su_kien_khong_uoc_e_trong}")
    private String name;

    private String description;

    @NotNull(message = "{validation.thoi_gian_bat_au_khong_uoc_e_trong}")
    private LocalDateTime startTime;

    @NotNull(message = "{validation.thoi_gian_ket_thuc_khong_uoc_e_trong}")
    private LocalDateTime endTime;

    @Valid
    private List<FlashSaleItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlashSaleItemRequest {
        @NotNull
        private UUID variantId;

        @NotNull
        private BigDecimal flashPrice;

        @NotNull
        private Integer flashStock;
    }
}
