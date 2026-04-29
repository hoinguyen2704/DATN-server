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
public class EmailChangeRequest {

    @NotBlank(message = "{validation.new_email_is_required}")
    @Email(message = "{validation.email_is_not_valid}")
    private String newEmail;

    @NotBlank(message = "{validation.current_password_is_required}")
    private String currentPassword;
}
