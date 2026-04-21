package com.hoz.hozitech.web.socket;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.services.realtime.RealtimeEventEnvelope;
import com.hoz.hozitech.application.services.realtime.RealtimeSocketSessionRegistry;

@ExtendWith(MockitoExtension.class)
class RealtimeWebSocketHandlerTest {

    @Mock
    private RealtimeSocketSessionRegistry sessionRegistry;

    @Mock
    private ObjectMapper objectMapper;

    private RealtimeWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RealtimeWebSocketHandler(sessionRegistry, objectMapper);
    }

    @Test
    void afterConnectionEstablished_shouldRejectUnauthorizedSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of());

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status ->
                CloseStatus.POLICY_VIOLATION.getCode() == status.getCode()
                        && "Unauthorized".equals(status.getReason())));
        verify(sessionRegistry, never()).register(any());
    }

    @Test
    void afterConnectionEstablished_shouldRegisterAndSendConnectedEvent() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userId", "u-1", "role", "USER"));
        when(session.isOpen()).thenReturn(true);
        when(objectMapper.writeValueAsString(any(RealtimeEventEnvelope.class))).thenReturn("{\"type\":\"WS_CONNECTED\"}");

        handler.afterConnectionEstablished(session);

        verify(sessionRegistry).register(session);
        ArgumentCaptor<RealtimeEventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(RealtimeEventEnvelope.class);
        verify(objectMapper).writeValueAsString(envelopeCaptor.capture());
        verify(session).sendMessage(argThat(message -> "{\"type\":\"WS_CONNECTED\"}".equals(message.getPayload())));
        org.junit.jupiter.api.Assertions.assertEquals("WS_CONNECTED", envelopeCaptor.getValue().getType());
    }

    @Test
    void handleTextMessage_shouldReplyWithPongForPingPayload() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(objectMapper.writeValueAsString(any(RealtimeEventEnvelope.class))).thenReturn("{\"type\":\"PONG\"}");

        handler.handleTextMessage(session, new TextMessage("ping"));

        verify(session).sendMessage(argThat(message -> "{\"type\":\"PONG\"}".equals(message.getPayload())));
    }

    @Test
    void handleTransportError_andAfterConnectionClosed_shouldUnregisterSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        handler.handleTransportError(session, new RuntimeException("transport error"));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionRegistry, times(2)).unregister(session);
    }
}
