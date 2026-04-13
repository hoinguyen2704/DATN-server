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

    @NotBlank(message = "New email is required")
    @Email(message = "Email is not valid")
    private String newEmail;

    @NotBlank(message = "Current password is required")
    private String currentPassword;
}
