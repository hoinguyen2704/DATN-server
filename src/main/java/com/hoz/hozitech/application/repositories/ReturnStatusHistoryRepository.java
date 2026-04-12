package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, UUID> {
    List<ReturnStatusHistory> findByReturnRequestOrderByCreatedAtDesc(ReturnRequest returnRequest);
}
