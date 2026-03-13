package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.TicketMessageRepository;
import com.hoz.hozitech.application.repositories.TicketRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.TicketService;
import com.hoz.hozitech.domain.dtos.request.TicketMessageRequest;
import com.hoz.hozitech.domain.dtos.request.TicketRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketMessageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;
import com.hoz.hozitech.domain.entities.Ticket;
import com.hoz.hozitech.domain.entities.TicketMessage;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponse<TicketResponse> getMyTickets(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Ticket> tickets = ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(tickets.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public TicketResponse createTicket(UUID userId, TicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .subject(request.getSubject())
                .status("OPEN")
                .user(user)
                .build();

        ticket = ticketRepository.save(ticket);

        TicketMessage initialMessage = TicketMessage.builder()
                .senderType("USER")
                .content(request.getContent())
                .attachmentsJson(request.getAttachmentsJson())
                .ticket(ticket)
                .build();

        ticketMessageRepository.save(initialMessage);
        
        // Reload ticket to include messages list if necessary, or just rely on mappings next fetch.
        ticket.getMessages().add(initialMessage);

        return mapToResponse(ticket);
    }

    @Override
    public TicketResponse getTicketDetail(UUID userId, UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Ticket does not belong to you");
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse userReplyToTicket(UUID userId, UUID ticketId, TicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Ticket does not belong to you");
        }

        TicketMessage reply = TicketMessage.builder()
                .senderType("USER")
                .content(request.getContent())
                .attachmentsJson(request.getAttachmentsJson())
                .ticket(ticket)
                .build();

        ticketMessageRepository.save(reply);
        
        // Optionally update ticket status
        ticket.setStatus("OPEN");
        ticketRepository.save(ticket);
        ticket.getMessages().add(reply);

        return mapToResponse(ticket);
    }

    @Override
    public PageResponse<TicketResponse> getAllTickets(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Ticket> tickets;
        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            tickets = ticketRepository.findAll(pageable);
        }
        return PageResponse.of(tickets.map(this::mapToResponse));
    }

    @Override
    public TicketResponse getTicketByIdAdmin(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse adminReplyToTicket(UUID ticketId, TicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketMessage reply = TicketMessage.builder()
                .senderType("ADMIN")
                .content(request.getContent())
                .attachmentsJson(request.getAttachmentsJson())
                .ticket(ticket)
                .build();

        ticketMessageRepository.save(reply);

        ticket.setStatus("ANSWERED");
        ticketRepository.save(ticket);
        ticket.getMessages().add(reply);

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(UUID ticketId, String status) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        ticket.setStatus(status.toUpperCase());
        return mapToResponse(ticketRepository.save(ticket));
    }

    private String generateTicketNumber() {
        return "TKT" + System.currentTimeMillis() + (int) (Math.random() * 100);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .subject(ticket.getSubject())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .userId(ticket.getUser().getId())
                .userName(ticket.getUser().getFullName() != null ? ticket.getUser().getFullName() : ticket.getUser().getUserName())
                .userEmail(ticket.getUser().getEmail())
                .messages(ticket.getMessages() != null ? 
                        ticket.getMessages().stream().map(this::mapMessageToResponse).collect(Collectors.toList()) 
                        : null)
                .build();
    }

    private TicketMessageResponse mapMessageToResponse(TicketMessage msg) {
        return TicketMessageResponse.builder()
                .id(msg.getId())
                .senderType(msg.getSenderType())
                .content(msg.getContent())
                .attachmentsJson(msg.getAttachmentsJson())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
