package com.hoz.hozitech.config.utils;

import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalizedApiResponseFactory {

    private final LocalizationUtils localizationUtils;

    public <T> ApiResponse<T> success(String messageKey, T data) {
        return ApiResponse.success(localizationUtils.getLocalizedMessage(messageKey), data);
    }

    public ApiResponse<Void> success(String messageKey) {
        return ApiResponse.success(localizationUtils.getLocalizedMessage(messageKey));
    }
}
