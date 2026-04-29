package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "{validation.current_password_is_required}")
    private String currentPassword;

    @NotBlank(message = "{validation.new_password_is_required}")
    @Size(min = 6, message = "{validation.new_password_must_be_at_least_6_characters_long}")
    private String newPassword;
}
