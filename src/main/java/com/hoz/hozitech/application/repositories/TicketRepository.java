package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hoz.hozitech.domain.enums.TicketStatus;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    Page<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "messages" })
    Optional<Ticket> findDetailById(UUID id);

    @EntityGraph(attributePaths = { "user", "messages" })
    Optional<Ticket> findDetailByTicketNumber(String ticketNumber);

    Optional<Ticket> findByTicketNumber(String ticketNumber);
    
    boolean existsByTicketNumber(String ticketNumber);
}
