package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID>, JpaSpecificationExecutor<ReturnRequest> {

    boolean existsByReturnNumber(String returnNumber);

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    Optional<ReturnRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);
}
