package com.hoz.hozitech.domain.dtos.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
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
public class AdminCreateUserRequest {

    @NotBlank(message = "{validation.full_name_is_required}")
    private String fullName;

    @NotBlank(message = "{validation.email_is_required}")
    @Email(message = "{validation.email_should_be_valid}")
    private String email;

    @NotBlank(message = "{validation.phone_number_is_required}")
    private String phoneNumber;

    @NotBlank(message = "{validation.password_is_required}")
    private String password;

    private LocalDate dateOfBirth;

    private String gender;

    @Size(max = 500, message = "{validation.avatar_url_must_be_at_most_500_characters}")
    private String avatarUrl;

    private String role;
}
