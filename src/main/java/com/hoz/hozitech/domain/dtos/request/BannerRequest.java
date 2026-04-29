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
    
    @NotBlank(message = "{validation.image_url_is_required}")
    private String imageUrl;
    
    private String targetUrl;
    private Integer sortOrder;
    private Boolean isActive;
}
