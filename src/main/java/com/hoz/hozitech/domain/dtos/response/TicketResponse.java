package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private String subject;
    private String status;
    private LocalDateTime createdAt;
    
    private UUID userId;
    private String userName;
    private String userEmail;
    
    private List<TicketMessageResponse> messages;
}
