package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginExchangeResponse {
    private String accessToken;
    private String refreshToken;
    private AuthResponse.UserDto user;
    private String redirectTo;
}
