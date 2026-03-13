package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerRequest {
    private String title;
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    private String targetUrl;
    private Integer sortOrder;
    private Boolean isActive;
}
