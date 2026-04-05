package com.hoz.hozitech.application.services.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPushService {

    private final RealtimeSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void sendToUser(UUID userId, String type, Object data) {
        send(sessionRegistry.getUserSessions(userId), type, data);
    }

    public void sendToAdmins(String type, Object data) {
        send(sessionRegistry.getAdminSessions(), type, data);
    }

    private void send(Collection<WebSocketSession> sessions, String type, Object data) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(
                    RealtimeEventEnvelope.builder()
                            .type(type)
                            .data(data)
                            .build()
            );
        } catch (JsonProcessingException ex) {
            log.warn("realtime_serialize_failed type={}", type, ex);
            return;
        }

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException ex) {
                sessionRegistry.unregister(session);
                log.debug("realtime_send_failed sessionId={}", session.getId(), ex);
            }
        }
    }
}
