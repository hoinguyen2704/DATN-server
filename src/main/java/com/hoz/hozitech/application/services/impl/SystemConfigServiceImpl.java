package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.SystemConfigRepository;
import com.hoz.hozitech.application.services.SystemConfigService;
import com.hoz.hozitech.domain.dtos.request.SystemConfigRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.SystemConfigResponse;
import com.hoz.hozitech.domain.entities.SystemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public PageResponse<SystemConfigResponse> getAllConfigs(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("configKey").ascending());
        Page<SystemConfig> configs = systemConfigRepository.findAll(pageable);
        return PageResponse.of(configs.map(this::mapToResponse));
    }

    @Override
    public SystemConfigResponse getConfigByKey(String key) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found"));
        return mapToResponse(config);
    }

    @Override
    @Transactional
    public SystemConfigResponse saveOrUpdateConfig(SystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(request.getConfigKey())
                .orElseGet(() -> SystemConfig.builder()
                        .configKey(request.getConfigKey())
                        .build());
                        
        config.setConfigValue(request.getConfigValue());
        config.setDescription(request.getDescription());
        
        return mapToResponse(systemConfigRepository.save(config));
    }

    @Override
    @Transactional
    public void deleteConfig(UUID id) {
        systemConfigRepository.deleteById(id);
    }

    private SystemConfigResponse mapToResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .description(config.getDescription())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
