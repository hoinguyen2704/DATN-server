package com.hoz.hozitech.config.websocket;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.hoz.hozitech.security.JwtWebSocketAuthInterceptor;
import com.hoz.hozitech.web.socket.RealtimeWebSocketHandler;

class RealtimeWebSocketConfigTest {

    @Mock
    private RealtimeWebSocketHandler realtimeWebSocketHandler;

    @Mock
    private JwtWebSocketAuthInterceptor jwtWebSocketAuthInterceptor;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    private RealtimeWebSocketConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new RealtimeWebSocketConfig(realtimeWebSocketHandler, jwtWebSocketAuthInterceptor);
        ReflectionTestUtils.setField(config, "allowedOrigins", " http://localhost:3000 , http://127.0.0.1:3000 ");

        when(registry.addHandler(realtimeWebSocketHandler, "/ws/support")).thenReturn(registration);
        when(registration.addInterceptors(jwtWebSocketAuthInterceptor)).thenReturn(registration);
    }

    @Test
    void registerWebSocketHandlers_shouldRegisterHandlerInterceptorAndTrimmedOrigins() {
        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(realtimeWebSocketHandler, "/ws/support");
        verify(registration).addInterceptors(jwtWebSocketAuthInterceptor);
        verify(registration).setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000");
    }
}
