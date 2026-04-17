package com.hoz.hozitech.security;

import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.domain.entities.User;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginIdentifierResolver {

    private final UserRepository userRepository;

    public Optional<User> resolve(String rawIdentifier) {
        String identifier = normalizeIdentifier(rawIdentifier);
        if (identifier.isBlank()) {
            return Optional.empty();
        }

        Optional<User> user = userRepository.findByEmailOrUserName(identifier, identifier);
        if (user.isPresent()) {
            return user;
        }

        for (String phoneCandidate : buildPhoneCandidates(identifier)) {
            Optional<User> byPhone = userRepository.findByPhoneNumber(phoneCandidate);
            if (byPhone.isPresent()) {
                return byPhone;
            }
        }

        return Optional.empty();
    }

    private String normalizeIdentifier(String rawIdentifier) {
        return rawIdentifier == null ? "" : rawIdentifier.trim();
    }

    private Set<String> buildPhoneCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String compact = identifier.replaceAll("[\\s().-]", "");

        if (compact.isBlank() || !compact.matches("^\\+?\\d+$")) {
            return candidates;
        }

        candidates.add(compact);

        if (compact.startsWith("+84") && compact.length() == 12) {
            candidates.add("0" + compact.substring(3));
            candidates.add(compact.substring(1));
            return candidates;
        }

        if (compact.startsWith("84") && compact.length() == 11) {
            candidates.add("+".concat(compact));
            candidates.add("0" + compact.substring(2));
            return candidates;
        }

        if (compact.startsWith("0") && compact.length() == 10) {
            candidates.add("+84" + compact.substring(1));
            candidates.add("84" + compact.substring(1));
        }

        return candidates;
    }
}
