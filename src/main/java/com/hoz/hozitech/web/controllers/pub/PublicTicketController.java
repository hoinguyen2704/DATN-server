package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.services.ticket.TicketService;
import com.hoz.hozitech.domain.dtos.request.ContactRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.TicketResponse;
import com.hoz.hozitech.web.base.RestAPI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestAPI("${api.prefix-client}/public/contact")
@RequiredArgsConstructor
public class PublicTicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> submitContactForm(
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Contact form submitted successfully",
                ticketService.createGuestTicket(request)
        ));
    }
}
