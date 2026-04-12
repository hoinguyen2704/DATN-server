package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserSocialAccount> findByUserIdAndProvider(UUID userId, String provider);

    List<UserSocialAccount> findByUserId(UUID userId);

    long countByUserId(UUID userId);
}
