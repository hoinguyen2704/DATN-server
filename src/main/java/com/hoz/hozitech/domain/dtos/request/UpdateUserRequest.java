package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    @NotBlank(message = "{validation.full_name_is_required}")
    @Size(min = 3, max = 100, message = "{validation.full_name_must_be_between_3_and_100_characters}")
    private String fullName;

    private LocalDate dateOfBirth;

    @Size(max = 10, message = "{validation.gender_max_length_is_10}")
    private String gender;

    @Size(max = 20, message = "{validation.phone_number_must_be_at_most_20_characters}")
    private String phoneNumber;

    // Note: Email still uses a separate OTP verification flow.
}
