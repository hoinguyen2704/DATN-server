package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse {
    private UUID id;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
