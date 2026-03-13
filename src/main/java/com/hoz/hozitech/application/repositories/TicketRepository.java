package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    Page<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Ticket> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    boolean existsByTicketNumber(String ticketNumber);
}
