package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    Optional<OtpToken> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);
}
