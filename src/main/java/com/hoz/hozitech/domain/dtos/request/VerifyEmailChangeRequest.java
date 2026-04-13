package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "Email is not valid")
    private String newEmail;

    @NotBlank(message = "OTP code is required")
    private String otpCode;
}
