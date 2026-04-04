package com.hoz.hozitech.application.services.setting;

import com.hoz.hozitech.domain.dtos.request.SettingRequest;
import com.hoz.hozitech.domain.dtos.response.SettingResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SettingService {

    Map<String, List<SettingResponse>> getAllSettings();

    List<SettingResponse> getSettingsByGroup(String groupName);

    String getSettingValue(String key);

    boolean getSettingBoolean(String key);

    BigDecimal getSettingNumber(String key);

    void batchUpdate(List<SettingRequest> requests);
}
