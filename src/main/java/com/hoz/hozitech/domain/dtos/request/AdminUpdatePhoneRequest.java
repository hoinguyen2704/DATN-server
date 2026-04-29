package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdatePhoneRequest {

    @NotBlank(message = "{validation.phone_number_is_required}")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "{validation.invalid_vietnamese_phone_number_format}")
    private String phoneNumber;

    @NotBlank(message = "{validation.reason_is_required}")
    @Size(max = 255, message = "{validation.reason_must_be_at_most_255_characters}")
    private String reason;
}
