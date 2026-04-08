package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {

    Optional<RefundTransaction> findByIdempotencyKey(String idempotencyKey);
}
