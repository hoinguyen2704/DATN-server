package com.hoz.hozitech.web.socket;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.services.realtime.RealtimeEventEnvelope;
import com.hoz.hozitech.application.services.realtime.RealtimeSocketSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!(session.getAttributes().get("userId") instanceof String)
                || !(session.getAttributes().get("role") instanceof String)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }

        sessionRegistry.register(session);
        String payload = objectMapper.writeValueAsString(
                RealtimeEventEnvelope.builder()
                        .type("WS_CONNECTED")
                        .data(Map.of("connected", true))
                        .build()
        );
        session.sendMessage(new TextMessage(payload));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            String payload = objectMapper.writeValueAsString(
                    RealtimeEventEnvelope.builder()
                            .type("PONG")
                            .data(Map.of("ok", true))
                            .build()
            );
            session.sendMessage(new TextMessage(payload));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(session);
        log.debug("realtime_transport_error sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionRegistry.unregister(session);
    }
}
