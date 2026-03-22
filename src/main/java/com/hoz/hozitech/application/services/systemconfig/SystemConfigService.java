package com.hoz.hozitech.application.services.systemconfig;

import com.hoz.hozitech.domain.dtos.request.SystemConfigRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.SystemConfigResponse;

import java.util.UUID;

public interface SystemConfigService {
    
    PageResponse<SystemConfigResponse> getAllConfigs(int page, int size);
    
    SystemConfigResponse getConfigByKey(String key);
    
    SystemConfigResponse saveOrUpdateConfig(SystemConfigRequest request);
    
    void deleteConfig(UUID id);
}
