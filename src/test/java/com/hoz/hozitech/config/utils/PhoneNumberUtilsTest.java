package com.hoz.hozitech.config.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PhoneNumberUtilsTest {

    @Test
    void normalizesVietnamesePhoneNumbersToZeroPrefixedFormat() {
        assertEquals("0912345678",
                PhoneNumberUtils.normalizeVietnamesePhoneNumber("+84 912 345 678").orElseThrow());
        assertEquals("0912345678",
                PhoneNumberUtils.normalizeVietnamesePhoneNumber("84912345678").orElseThrow());
    }

    @Test
    void buildsLookupCandidatesAcrossSupportedFormats() {
        Set<String> candidates = PhoneNumberUtils.buildLookupCandidates("0912345678");

        assertTrue(candidates.contains("0912345678"));
        assertTrue(candidates.contains("+84912345678"));
        assertTrue(candidates.contains("84912345678"));
    }
}
