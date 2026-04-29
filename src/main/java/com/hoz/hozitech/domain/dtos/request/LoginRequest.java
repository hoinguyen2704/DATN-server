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
    @NotBlank(message = "{validation.login_identifier_is_required}")
    private String identifier;

    @NotBlank(message = "{validation.password_is_required}")
    private String password;
}
