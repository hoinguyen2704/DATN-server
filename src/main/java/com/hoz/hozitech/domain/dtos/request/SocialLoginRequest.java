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
public class SocialLoginRequest {
    @NotBlank(message = "Provider is required (GOOGLE or FACEBOOK)")
    private String provider;

    @NotBlank(message = "Token is required")
    private String token;
}
