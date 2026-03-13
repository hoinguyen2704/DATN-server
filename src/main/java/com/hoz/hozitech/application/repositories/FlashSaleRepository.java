package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.FlashSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    @Query("SELECT fs FROM FlashSale fs WHERE fs.status = 'ACTIVE' " +
            "AND fs.startTime <= CURRENT_TIMESTAMP AND fs.endTime >= CURRENT_TIMESTAMP")
    Optional<FlashSale> findActiveFlashSale();

    Page<FlashSale> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
