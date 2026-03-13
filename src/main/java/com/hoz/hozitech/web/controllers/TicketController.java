package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.TicketService;
import com.hoz.hozitech.config.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.TicketMessageRequest;
import com.hoz.hozitech.domain.dtos.request.TicketRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-client}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch tickets successfully", 
                ticketService.getMyTickets(userDetails.getUser().getId(), page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket created successfully", 
                ticketService.createTicket(userDetails.getUser().getId(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch ticket successfully", 
                ticketService.getTicketDetail(userDetails.getUser().getId(), id)));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<TicketResponse>> replyToTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TicketMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Replied to ticket successfully", 
                ticketService.userReplyToTicket(userDetails.getUser().getId(), id, request)));
    }
}
