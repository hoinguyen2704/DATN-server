package com.hoz.hozitech.application.services.realtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RealtimeSocketSessionRegistry {

    private final ConcurrentMap<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<String>> userSessionIds = new ConcurrentHashMap<>();
    private final Set<String> adminSessionIds = ConcurrentHashMap.newKeySet();

    public void register(WebSocketSession session) {
        Object userIdRaw = session.getAttributes().get("userId");
        Object roleRaw = session.getAttributes().get("role");
        if (!(userIdRaw instanceof String userIdStr) || !(roleRaw instanceof String role)) {
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException ex) {
            return;
        }

        String sessionId = session.getId();
        sessionsById.put(sessionId, session);
        userSessionIds.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);

        if ("ADMIN".equalsIgnoreCase(role)) {
            adminSessionIds.add(sessionId);
        }
    }

    public void unregister(WebSocketSession session) {
        String sessionId = session.getId();
        sessionsById.remove(sessionId);
        adminSessionIds.remove(sessionId);

        Object userIdRaw = session.getAttributes().get("userId");
        if (!(userIdRaw instanceof String userIdStr)) {
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            Set<String> userSessions = userSessionIds.get(userId);
            if (userSessions == null) {
                return;
            }
            userSessions.remove(sessionId);
            if (userSessions.isEmpty()) {
                userSessionIds.remove(userId);
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed user id
        }
    }

    public List<WebSocketSession> getUserSessions(UUID userId) {
        Set<String> sessionIds = userSessionIds.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return collectOpenSessions(sessionIds);
    }

    public List<WebSocketSession> getAdminSessions() {
        if (adminSessionIds.isEmpty()) {
            return List.of();
        }
        return collectOpenSessions(adminSessionIds);
    }

    private List<WebSocketSession> collectOpenSessions(Set<String> sessionIds) {
        List<WebSocketSession> openSessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessionsById.get(sessionId);
            if (session == null) {
                continue;
            }
            if (session.isOpen()) {
                openSessions.add(session);
            } else {
                unregister(session);
            }
        }
        return openSessions;
    }
}
