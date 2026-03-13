package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfigRequest {
    
    @NotBlank(message = "Config config_key is required")
    private String configKey;
    
    private String configValue;
    
    private String description;
}
