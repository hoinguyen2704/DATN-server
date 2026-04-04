package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingRequest {

    @NotBlank(message = "Setting key is required")
    private String settingKey;

    @NotBlank(message = "Setting value is required")
    private String settingValue;
}
