package com.hoz.hozitech.config.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.text.Normalizer;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LocalizationUtils {
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public String getLocalizedMessage(String messageKey, Object... params) {
        return messageSource.getMessage(normalizeMessageKey(messageKey), params, resolveLocale());
    }

    public String getLocalizedMessageOrDefault(String messageKey, String defaultMessage, Object... params) {
        String normalizedMessageKey = normalizeMessageKey(messageKey);
        if (normalizedMessageKey == null) {
            return defaultMessage;
        }
        String fallback = defaultMessage != null ? defaultMessage : normalizedMessageKey;
        return messageSource.getMessage(normalizedMessageKey, params, fallback, resolveLocale());
    }

    public String localizeErrorMessage(String rawMessage, Object... params) {
        return localizeLiteralMessage("error.literal", rawMessage, params);
    }

    public String localizeValidationMessage(String rawMessage, Object... params) {
        return localizeLiteralMessage("validation.literal", rawMessage, params);
    }

    private String localizeLiteralMessage(String prefix, String rawMessage, Object... params) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return rawMessage;
        }
        String messageKey = buildLiteralMessageKey(prefix, rawMessage);
        return messageSource.getMessage(messageKey, params, rawMessage, resolveLocale());
    }

    private Locale resolveLocale() {
        try {
            HttpServletRequest request = WebUtils.getCurrentRequest();
            if (request != null) {
                return localeResolver.resolveLocale(request);
            }
        } catch (IllegalStateException ignored) {
            // No request context is available.
        }
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.forLanguageTag("vi-VN");
    }

    private String normalizeMessageKey(String messageKey) {
        if (messageKey == null) {
            return null;
        }
        String trimmed = messageKey.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildLiteralMessageKey(String prefix, String rawMessage) {
        String normalized = Normalizer.normalize(rawMessage, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return prefix + "." + (normalized.isBlank() ? "unknown" : normalized);
    }
}
