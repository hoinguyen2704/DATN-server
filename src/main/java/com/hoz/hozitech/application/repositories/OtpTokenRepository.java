package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    Optional<OtpToken> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);

    @Modifying
    @Query("UPDATE OtpToken o SET o.isUsed = true WHERE o.email = :email AND o.isUsed = false")
    void invalidateAllByEmail(@Param("email") String email);
}
