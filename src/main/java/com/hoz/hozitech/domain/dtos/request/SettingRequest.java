package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingRequest {

    @NotBlank(message = "{validation.setting_key_is_required}")
    private String settingKey;

    @NotBlank(message = "{validation.setting_value_is_required}")
    private String settingValue;
}
