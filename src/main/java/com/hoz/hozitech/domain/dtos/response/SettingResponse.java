package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingResponse {
    private String id;
    private String groupName;
    private String settingKey;
    private String settingValue;
    private String valueType;
    private String description;
    private LocalDateTime updatedAt;
}
