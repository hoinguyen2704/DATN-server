package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVariantAttributeRequest {
    @NotBlank(message = "Variant attribute name is required")
    private String name;

    @NotBlank(message = "Variant attribute option labels are required")
    private String optionLabelsText;
}
