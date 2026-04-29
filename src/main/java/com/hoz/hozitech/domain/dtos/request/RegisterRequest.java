package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "{validation.full_name_is_required}")
    @Size(min = 3, max = 100, message = "{validation.full_name_must_be_between_3_and_100_characters}")
    private String fullName;

    @NotBlank(message = "{validation.username_is_required}")
    @Size(min = 3, max = 50, message = "{validation.username_must_be_between_3_and_50_characters}")
    private String userName;

    @NotBlank(message = "{validation.email_is_required}")
    @Email(message = "{validation.email_should_be_valid}")
    private String email;

    @NotBlank(message = "{validation.phone_number_is_required}")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "{validation.invalid_vietnamese_phone_number_format}")
    private String phoneNumber;

    @NotBlank(message = "{validation.password_is_required}")
    @Size(min = 6, message = "{validation.password_must_be_at_least_6_characters}")
    private String password;
}
