package com.hoz.hozitech.application.services.user;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsernameAllocator {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int GENERATED_USERNAME_BASE_MAX_LENGTH = 40;

    private final UserRepository userRepository;

    public String generateUniqueUsername(String email) {
        return generateUniqueUsername(email, null);
    }

    public String generateUniqueUsername(String email, UUID excludedUserId) {
        String emailLocalPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String sanitizedBase = emailLocalPart.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[._-]+|[._-]+$", "");

        if (sanitizedBase.isBlank()) {
            sanitizedBase = "customer";
        }
        if (sanitizedBase.length() < 3) {
            sanitizedBase = "user-" + sanitizedBase;
        }
        if (sanitizedBase.length() > GENERATED_USERNAME_BASE_MAX_LENGTH) {
            sanitizedBase = sanitizedBase.substring(0, GENERATED_USERNAME_BASE_MAX_LENGTH);
        }

        String candidate = sanitizedBase;
        int suffix = 1;
        while (isUsernameTaken(candidate, excludedUserId)) {
            String suffixText = "-" + suffix++;
            int maxBaseLength = Math.max(3, MAX_USERNAME_LENGTH - suffixText.length());
            String base = sanitizedBase.length() > maxBaseLength
                    ? sanitizedBase.substring(0, maxBaseLength)
                    : sanitizedBase;
            candidate = base + suffixText;
        }
        return candidate;
    }

    private boolean isUsernameTaken(String candidate, UUID excludedUserId) {
        return userRepository.findByUserName(candidate)
                .filter(existing -> excludedUserId == null || !existing.getId().equals(excludedUserId))
                .isPresent();
    }
}
