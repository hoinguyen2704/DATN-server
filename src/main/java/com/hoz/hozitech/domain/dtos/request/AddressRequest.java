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
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {
    @NotBlank(message = "{validation.full_name_is_required}")
    @Size(min = 2, max = 100, message = "{validation.full_name_must_be_between_2_and_100_characters}")
    private String fullName;

    @NotBlank(message = "{validation.phone_number_is_required}")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "{validation.invalid_vietnamese_phone_number_format}")
    private String phoneNumber;

    @NotBlank(message = "{validation.province_is_required}")
    private String province;

    @NotBlank(message = "{validation.district_is_required}")
    private String district;

    @NotBlank(message = "{validation.ward_is_required}")
    private String ward;

    @NotBlank(message = "{validation.detail_address_is_required}")
    private String detailAddress;

    private Boolean isDefault;
}
