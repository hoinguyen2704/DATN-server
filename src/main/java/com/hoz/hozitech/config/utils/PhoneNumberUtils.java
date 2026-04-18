package com.hoz.hozitech.config.utils;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static Optional<String> normalizeVietnamesePhoneNumber(String rawPhoneNumber) {
        String compact = compactPhoneNumber(rawPhoneNumber);
        if (compact.isBlank()) {
            return Optional.empty();
        }

        if (compact.startsWith("+84") && compact.length() == 12) {
            compact = "0" + compact.substring(3);
        } else if (compact.startsWith("84") && compact.length() == 11) {
            compact = "0" + compact.substring(2);
        }

        if (!compact.matches("^0[35789][0-9]{8}$")) {
            return Optional.empty();
        }

        return Optional.of(compact);
    }

    public static Set<String> buildLookupCandidates(String rawPhoneNumber) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String compact = compactPhoneNumber(rawPhoneNumber);
        if (compact.isBlank() || !compact.matches("^\\+?\\d+$")) {
            return candidates;
        }

        candidates.add(compact);

        normalizeVietnamesePhoneNumber(rawPhoneNumber).ifPresent(normalized -> {
            candidates.add(normalized);
            candidates.add("+84" + normalized.substring(1));
            candidates.add("84" + normalized.substring(1));
        });

        return candidates;
    }

    private static String compactPhoneNumber(String rawPhoneNumber) {
        return rawPhoneNumber == null ? "" : rawPhoneNumber.trim().replaceAll("[\\s().-]", "");
    }
}
