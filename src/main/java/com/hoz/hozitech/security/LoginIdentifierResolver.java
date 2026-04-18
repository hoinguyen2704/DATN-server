package com.hoz.hozitech.security;

import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.config.utils.PhoneNumberUtils;
import com.hoz.hozitech.domain.entities.User;
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
        return PhoneNumberUtils.buildLookupCandidates(identifier);
    }
}
