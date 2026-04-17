package com.hoz.hozitech.domain.dtos.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @JsonAlias("email")
    @NotBlank(message = "Login identifier is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
