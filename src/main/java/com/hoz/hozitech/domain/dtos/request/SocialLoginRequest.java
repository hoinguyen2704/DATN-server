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
    @NotBlank(message = "{validation.provider_is_required_google}")
    private String provider;

    @NotBlank(message = "{validation.token_is_required}")
    private String token;
}
