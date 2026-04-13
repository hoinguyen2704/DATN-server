package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.EmailChangeOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailChangeOtpRepository extends JpaRepository<EmailChangeOtp, UUID> {

    @Modifying
    @Query("UPDATE EmailChangeOtp e SET e.isUsed = true WHERE e.user.id = :userId AND e.isUsed = false")
    void invalidateAllByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT e FROM EmailChangeOtp e
            WHERE e.user.id = :userId
                AND LOWER(e.newEmail) = LOWER(:newEmail)
                AND e.otpCode = :otpCode
                AND e.isUsed = false
            """)
    Optional<EmailChangeOtp> findValidOtp(@Param("userId") UUID userId,
                                          @Param("newEmail") String newEmail,
                                          @Param("otpCode") String otpCode);

    @Query("""
            SELECT e FROM EmailChangeOtp e
            WHERE e.user.id = :userId
                AND LOWER(e.newEmail) = LOWER(:newEmail)
                AND e.isUsed = false
            ORDER BY e.createdAt DESC
            """)
    List<EmailChangeOtp> findPendingRequests(@Param("userId") UUID userId, @Param("newEmail") String newEmail);

    @Query("""
            SELECT COUNT(e) FROM EmailChangeOtp e
            WHERE e.user.id = :userId
                AND e.createdAt >= :since
            """)
    long countRecentRequests(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
