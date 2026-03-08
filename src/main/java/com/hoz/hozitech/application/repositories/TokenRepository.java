package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByToken(String token);

    List<Token> findByUserIdAndExpiredFalseAndRevokedFalse(UUID userId);
}
