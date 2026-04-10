package com.hoz.hozitech.application.services.ticket;

import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.constant.RealtimeEventType;
import com.hoz.hozitech.application.repositories.TicketMessageRepository;
import com.hoz.hozitech.application.repositories.TicketRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.realtime.RealtimeEventPushService;
import com.hoz.hozitech.domain.dtos.request.ContactRequest;
import com.hoz.hozitech.domain.dtos.request.TicketMessageRequest;
import com.hoz.hozitech.domain.dtos.request.TicketRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketMessageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;
import com.hoz.hozitech.domain.entities.Ticket;
import com.hoz.hozitech.domain.entities.TicketMessage;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.TicketStatus;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeEventPushService realtimeEventPushService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getMyTickets(UUID userId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
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
                .status(TicketStatus.OPEN)
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
        publishToUserAndAdmins(RealtimeEventType.SUPPORT_TICKET_CREATED, ticket, initialMessage);

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse createGuestTicket(ContactRequest request) {
        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .subject(request.getSubject())
                .status(TicketStatus.OPEN)
                .guestName(request.getName())
                .guestEmail(request.getEmail())
                .guestPhone(request.getPhone())
                .build();

        ticket = ticketRepository.save(ticket);

        TicketMessage initialMessage = TicketMessage.builder()
                .senderType("USER")
                .content(request.getMessage())
                .ticket(ticket)
                .build();

        ticketMessageRepository.save(initialMessage);
        ticket.getMessages().add(initialMessage);
        publishToAdmins(RealtimeEventType.SUPPORT_TICKET_CREATED, ticket, initialMessage);

        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketDetail(UUID userId, UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Ticket does not belong to you");
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse userReplyToTicket(UUID userId, UUID ticketId, TicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Ticket does not belong to you");
        }

        TicketMessage reply = TicketMessage.builder()
                .senderType("USER")
                .content(request.getContent())
                .attachmentsJson(request.getAttachmentsJson())
                .ticket(ticket)
                .build();

        ticketMessageRepository.save(reply);
        
        // Optionally update ticket status
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);
        ticket.getMessages().add(reply);
        publishToUserAndAdmins(RealtimeEventType.SUPPORT_MESSAGE_CREATED, ticket, reply);

        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getAllTickets(String status, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Ticket> tickets;
        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(TicketStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            tickets = ticketRepository.findAll(pageable);
        }
        return PageResponse.of(tickets.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
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

        ticket.setStatus(TicketStatus.ANSWERED);
        ticketRepository.save(ticket);
        ticket.getMessages().add(reply);
        publishToAdmins(RealtimeEventType.SUPPORT_MESSAGE_CREATED, ticket, reply);
        publishToUser(RealtimeEventType.SUPPORT_MESSAGE_CREATED, ticket, reply);
        if (ticket.getUser() != null) {
            notificationService.createForUser(
                    ticket.getUser().getId(),
                    "Bạn có phản hồi hỗ trợ mới",
                    "Yêu cầu " + ticket.getTicketNumber() + " đã được admin phản hồi.",
                    "SUPPORT"
            );
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(UUID ticketId, String status) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        TicketStatus oldStatus = ticket.getStatus();

        TicketStatus newStatus = TicketStatus.valueOf(status.toUpperCase());
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        if (ticket.getUser() != null && oldStatus != newStatus) {
            notificationService.createForUser(
                    ticket.getUser().getId(),
                    "Cập nhật trạng thái hỗ trợ",
                    "Yêu cầu " + ticket.getTicketNumber() + " đã chuyển sang trạng thái " + newStatus.getDescription() + ".",
                    "SUPPORT"
            );
        }
        publishToAdmins(RealtimeEventType.SUPPORT_STATUS_UPDATED, saved, null);
        publishToUser(RealtimeEventType.SUPPORT_STATUS_UPDATED, saved, null);
        return mapToResponse(saved);
    }

    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void publishToUserAndAdmins(String eventType, Ticket ticket, TicketMessage message) {
        publishToAdmins(eventType, ticket, message);
        publishToUser(eventType, ticket, message);
    }

    private void publishToAdmins(String eventType, Ticket ticket, TicketMessage message) {
        realtimeEventPushService.sendToAdmins(eventType, buildRealtimePayload(ticket, message));
    }

    private void publishToUser(String eventType, Ticket ticket, TicketMessage message) {
        if (ticket.getUser() == null) {
            return;
        }
        realtimeEventPushService.sendToUser(ticket.getUser().getId(), eventType, buildRealtimePayload(ticket, message));
    }

    private Map<String, Object> buildRealtimePayload(Ticket ticket, TicketMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("ticketNumber", ticket.getTicketNumber());
        payload.put("subject", ticket.getSubject());
        payload.put("status", ticket.getStatus() != null ? ticket.getStatus().name() : null);
        payload.put("userId", ticket.getUser() != null ? ticket.getUser().getId() : null);
        payload.put("userName", resolveTicketOwnerName(ticket));

        if (message != null) {
            payload.put("senderType", message.getSenderType());
            payload.put("messageId", message.getId());
            payload.put("messagePreview", abbreviateMessage(message.getContent()));
            payload.put("messageCreatedAt", message.getCreatedAt());
        }
        return payload;
    }

    private String resolveTicketOwnerName(Ticket ticket) {
        if (ticket.getUser() != null) {
            return ticket.getUser().getFullName() != null
                    ? ticket.getUser().getFullName()
                    : ticket.getUser().getUserName();
        }
        return ticket.getGuestName() != null ? ticket.getGuestName() : "Khách";
    }

    private String abbreviateMessage(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 157) + "...";
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        String uName = ticket.getUser() != null ? 
            (ticket.getUser().getFullName() != null ? ticket.getUser().getFullName() : ticket.getUser().getUserName()) 
            : ticket.getGuestName() != null ? ticket.getGuestName() : "Khách";
            
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .subject(ticket.getSubject())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .userId(ticket.getUser() != null ? ticket.getUser().getId() : null)
                .userName(uName)
                .userEmail(ticket.getUser() != null ? ticket.getUser().getEmail() : ticket.getGuestEmail())
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
