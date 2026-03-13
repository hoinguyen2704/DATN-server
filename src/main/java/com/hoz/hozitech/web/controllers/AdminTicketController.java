package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.TicketService;
import com.hoz.hozitech.domain.dtos.request.TicketMessageRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-admin}/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch tickets successfully", ticketService.getAllTickets(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch ticket successfully", ticketService.getTicketByIdAdmin(id)));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<TicketResponse>> adminReplyToTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TicketMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Replied to ticket successfully", ticketService.adminReplyToTicket(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicketStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated", ticketService.updateTicketStatus(id, body.get("status"))));
    }
}
