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
public class BrandRequest {

    @NotBlank(message = "{validation.brand_name_is_required}")
    @Size(max = 100, message = "{validation.brand_name_must_be_less_than_100_characters}")
    private String name;

    @Size(max = 500, message = "{validation.logo_url_must_be_less_than_500_characters}")
    private String logoUrl;
}
