package com.hoz.hozitech.application.services.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class RealtimeSocketSessionRegistryTest {

    private RealtimeSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RealtimeSocketSessionRegistry();
    }

    @Test
    void register_shouldStoreUserAndAdminSessions() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WebSocketSession adminSession = buildSession("admin-1", adminId, "ADMIN", true);
        WebSocketSession userSession = buildSession("user-1", userId, "USER", true);

        registry.register(adminSession);
        registry.register(userSession);

        assertEquals(1, registry.getAdminSessions().size());
        assertEquals(1, registry.getUserSessions(adminId).size());
        assertEquals(1, registry.getUserSessions(userId).size());
    }

    @Test
    void register_shouldIgnoreSessionsWithMissingOrMalformedAttributes() {
        WebSocketSession missingRole = buildSession("missing-role", UUID.randomUUID(), null, true);
        WebSocketSession missingUserId = buildSession("missing-user", null, "USER", true);
        WebSocketSession malformedUserId = buildSessionWithRawAttributes(
                "bad-user-id",
                Map.of("userId", "not-a-uuid", "role", "ADMIN"),
                true);

        registry.register(missingRole);
        registry.register(missingUserId);
        registry.register(malformedUserId);

        assertTrue(registry.getAdminSessions().isEmpty());
        assertTrue(registry.getUserSessions(UUID.randomUUID()).isEmpty());
    }

    @Test
    void getSessions_shouldDropClosedSessionsFromRegistry() {
        UUID userId = UUID.randomUUID();
        WebSocketSession closedSession = buildSession("closed-1", userId, "USER", false);

        registry.register(closedSession);

        assertTrue(registry.getUserSessions(userId).isEmpty());
        assertTrue(registry.getUserSessions(userId).isEmpty());
    }

    @Test
    void unregister_shouldRemoveSessionFromUserAndAdminIndexes() {
        UUID adminId = UUID.randomUUID();
        WebSocketSession adminSession = buildSession("admin-2", adminId, "ADMIN", true);
        registry.register(adminSession);

        registry.unregister(adminSession);

        assertTrue(registry.getAdminSessions().isEmpty());
        assertTrue(registry.getUserSessions(adminId).isEmpty());
    }

    private WebSocketSession buildSession(String sessionId, UUID userId, String role, boolean open) {
        Map<String, Object> attributes = new HashMap<>();
        if (userId != null) {
            attributes.put("userId", userId.toString());
        }
        if (role != null) {
            attributes.put("role", role);
        }
        return buildSessionWithRawAttributes(sessionId, attributes, open);
    }

    private WebSocketSession buildSessionWithRawAttributes(String sessionId, Map<String, Object> attributes, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(open);
        return session;
    }
}
