package com.hoz.hozitech.application.services.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RealtimeEventPushServiceTest {

    @Mock
    private RealtimeSocketSessionRegistry sessionRegistry;

    @Mock
    private ObjectMapper objectMapper;

    private RealtimeEventPushService service;

    @BeforeEach
    void setUp() {
        service = new RealtimeEventPushService(sessionRegistry, objectMapper);
    }

    @Test
    void sendToUser_shouldSerializeEnvelopeAndPushToOpenSessions() throws Exception {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(sessionRegistry.getUserSessions(userId)).thenReturn(List.of(session));
        when(objectMapper.writeValueAsString(any(RealtimeEventEnvelope.class))).thenReturn("{\"type\":\"ORDER_UPDATED\"}");

        service.sendToUser(userId, "ORDER_UPDATED", Map.of("orderNumber", "ORD-001"));

        ArgumentCaptor<RealtimeEventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(RealtimeEventEnvelope.class);
        verify(objectMapper).writeValueAsString(envelopeCaptor.capture());
        RealtimeEventEnvelope envelope = envelopeCaptor.getValue();
        assertEquals("ORDER_UPDATED", envelope.getType());
        assertEquals("ORD-001", ((Map<?, ?>) envelope.getData()).get("orderNumber"));
        assertNotNull(envelope.getTimestamp());

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        assertEquals("{\"type\":\"ORDER_UPDATED\"}", messageCaptor.getValue().getPayload());
    }

    @Test
    void sendToAdmins_shouldUnregisterSessionWhenSendFails() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("admin-session");
        when(session.isOpen()).thenReturn(true);
        when(sessionRegistry.getAdminSessions()).thenReturn(List.of(session));
        when(objectMapper.writeValueAsString(any(RealtimeEventEnvelope.class))).thenReturn("{\"type\":\"ADMIN_ALERT\"}");
        doThrow(new IOException("socket closed")).when(session).sendMessage(any(TextMessage.class));

        service.sendToAdmins("ADMIN_ALERT", Map.of("severity", "HIGH"));

        verify(sessionRegistry).unregister(session);
    }

    @Test
    void sendToUser_shouldSkipSerializationWhenNoSessionsAvailable() throws Exception {
        UUID userId = UUID.randomUUID();
        when(sessionRegistry.getUserSessions(userId)).thenReturn(List.of());

        service.sendToUser(userId, "ORDER_UPDATED", Map.of("orderNumber", "ORD-002"));

        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void sendToUser_shouldIgnoreSerializationFailure() throws Exception {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(sessionRegistry.getUserSessions(userId)).thenReturn(List.of(session));
        when(objectMapper.writeValueAsString(any(RealtimeEventEnvelope.class)))
                .thenThrow(new JsonProcessingException("boom") { });

        service.sendToUser(userId, "ORDER_UPDATED", Map.of("orderNumber", "ORD-003"));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
