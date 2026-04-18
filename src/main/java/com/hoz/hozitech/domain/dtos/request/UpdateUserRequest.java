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
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    private LocalDate dateOfBirth;

    @Size(max = 10, message = "Gender max length is 10")
    private String gender;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    // Note: Email still uses a separate OTP verification flow.
}
