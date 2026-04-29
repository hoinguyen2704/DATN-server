package com.hoz.hozitech.application.services.ticket;

import com.hoz.hozitech.domain.dtos.request.ContactRequest;
import com.hoz.hozitech.domain.dtos.request.TicketMessageRequest;
import com.hoz.hozitech.domain.dtos.request.TicketRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;

import java.util.UUID;

public interface TicketService {
    // User endpoints
    PageResponse<TicketResponse> getMyTickets(UUID userId, int page, int size);
    
    TicketResponse createTicket(UUID userId, TicketRequest request);
    
    TicketResponse createGuestTicket(ContactRequest request);
    
    TicketResponse getTicketDetail(UUID userId, String ticketNumber);
    
    TicketResponse userReplyToTicket(UUID userId, String ticketNumber, TicketMessageRequest request);
    
    // Admin endpoints
    PageResponse<TicketResponse> getAllTickets(String status, int page, int size);
    
    TicketResponse getTicketByIdAdmin(UUID ticketId);
    
    TicketResponse adminReplyToTicket(UUID ticketId, TicketMessageRequest request);
    
    TicketResponse updateTicketStatus(UUID ticketId, String status);
}
