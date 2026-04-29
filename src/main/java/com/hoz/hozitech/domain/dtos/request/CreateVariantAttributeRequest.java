package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVariantAttributeRequest {
    @NotBlank(message = "{validation.variant_attribute_name_is_required}")
    private String name;

    @NotBlank(message = "{validation.variant_attribute_option_labels_are_required}")
    private String optionLabelsText;
}
