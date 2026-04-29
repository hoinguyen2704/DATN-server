package com.hoz.hozitech.application.services.setting;

import com.hoz.hozitech.application.repositories.SettingRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.domain.dtos.request.SettingRequest;
import com.hoz.hozitech.domain.dtos.response.SettingResponse;
import com.hoz.hozitech.domain.entities.Setting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;
    private final AdminNotificationService adminNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<SettingResponse>> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.groupingBy(SettingResponse::getGroupName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> getSettingsByGroup(String groupName) {
        return settingRepository.findByGroupName(groupName).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValue(String key) {
        return settingRepository.findBySettingKey(key)
                .map(Setting::getSettingValue)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getSettingBoolean(String key) {
        String value = getSettingValue(key);
        return "true".equalsIgnoreCase(value);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getSettingNumber(String key) {
        String value = getSettingValue(key);
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid number for setting key '{}': '{}'", key, value);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public void batchUpdate(List<SettingRequest> requests) {
        for (SettingRequest req : requests) {
            Setting setting = settingRepository.findBySettingKey(req.getSettingKey())
                    .orElseThrow(() -> new InvalidParamException("Setting not found: " + req.getSettingKey())
                            .withMessageKey("error.setting_not_found", req.getSettingKey()));
            setting.setSettingValue(req.getSettingValue());
            settingRepository.save(setting);
        }
        adminNotificationService.createShared(AdminNotificationTemplates.settingUpdated(requests.size()), true);
        log.info("Batch updated {} settings", requests.size());
    }

    private SettingResponse mapToResponse(Setting setting) {
        return SettingResponse.builder()
                .id(setting.getId().toString())
                .groupName(setting.getGroupName())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .valueType(setting.getValueType())
                .description(setting.getDescription())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
