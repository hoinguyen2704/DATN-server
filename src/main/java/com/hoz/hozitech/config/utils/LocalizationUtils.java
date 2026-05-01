package com.hoz.hozitech.config.utils;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalizationUtils {
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("vi-VN");

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public String getLocalizedMessage(String messageKey, Object... params) {
        return getLocalizedMessageForLocale(messageKey, resolveLocale(), params);
    }

    public String getLocalizedMessageOrDefault(String messageKey, String defaultMessage, Object... params) {
        return getLocalizedMessageOrDefaultForLocale(messageKey, resolveLocale(), defaultMessage, params);
    }

    public String getLocalizedMessageForLocale(String messageKey, Locale locale, Object... params) {
        String normalizedMessageKey = normalizeMessageKey(messageKey);
        if (normalizedMessageKey == null) {
            return null;
        }
        Locale targetLocale = locale != null ? locale : DEFAULT_LOCALE;
        try {
            return messageSource.getMessage(normalizedMessageKey, params, targetLocale);
        } catch (NoSuchMessageException ex) {
            if (!DEFAULT_LOCALE.equals(targetLocale)) {
                return messageSource.getMessage(normalizedMessageKey, params, normalizedMessageKey, DEFAULT_LOCALE);
            }
            throw ex;
        }
    }

    public String getLocalizedMessageOrDefaultForLocale(String messageKey, Locale locale, String defaultMessage,
            Object... params) {
        String normalizedMessageKey = normalizeMessageKey(messageKey);
        if (normalizedMessageKey == null) {
            return defaultMessage;
        }
        String fallback = defaultMessage != null ? defaultMessage : normalizedMessageKey;
        Locale targetLocale = locale != null ? locale : DEFAULT_LOCALE;
        String localized = messageSource.getMessage(normalizedMessageKey, params, null, targetLocale);
        if (localized != null) {
            return localized;
        }
        if (!DEFAULT_LOCALE.equals(targetLocale)) {
            return messageSource.getMessage(normalizedMessageKey, params, fallback, DEFAULT_LOCALE);
        }
        return fallback;
    }

    public String localizeErrorMessage(String rawMessage, Object... params) {
        return localizeLiteralMessage("error.literal", rawMessage, params);
    }

    public String localizeValidationMessage(String rawMessage, Object... params) {
        return localizeLiteralMessage("validation.literal", rawMessage, params);
    }

    public String resolveLocalizedRuntimeMessage(LocalizedRuntimeException ex, String fallbackMessage) {
        String messageKey = ex.getMessageKey() != null ? ex.getMessageKey() : fallbackMessage;
        String localized = getLocalizedMessageOrDefault(messageKey, fallbackMessage, ex.getMessageArgs());
        if (localized == null || localized.equals(fallbackMessage)) {
            return localizeErrorMessage(fallbackMessage);
        }
        return localized;
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
        return locale != null ? locale : DEFAULT_LOCALE;
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
