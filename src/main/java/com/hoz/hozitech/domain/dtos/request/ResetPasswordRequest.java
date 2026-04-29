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
public class ResetPasswordRequest {
    @NotBlank(message = "{validation.email_is_required}")
    @Email(message = "{validation.email_is_not_valid}")
    private String email;

    @NotBlank(message = "{validation.otp_code_is_required}")
    private String otpCode;

    @NotBlank(message = "{validation.new_password_is_required}")
    private String newPassword;
}
