package com.hoz.hozitech.application.services.realtime;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RealtimeEventEnvelope {
    private String type;
    private Object data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
