package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketMessageRequest {
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    private String attachmentsJson;
}
