package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketMessageResponse {
    private UUID id;
    private String senderType; // USER, ADMIN, AI_BOT
    private String content;
    private String attachmentsJson;
    private LocalDateTime createdAt;
}
