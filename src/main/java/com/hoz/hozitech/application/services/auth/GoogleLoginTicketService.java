package com.hoz.hozitech.application.services.auth;

import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class GoogleLoginTicketService {

    private static final int TICKET_SIZE_BYTES = 32;
    private static final long TICKET_TTL_SECONDS = 120;

    private final ConcurrentHashMap<String, TicketPayload> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(UUID userId, String redirectTo) {
        cleanupExpiredTickets();

        byte[] randomBytes = new byte[TICKET_SIZE_BYTES];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        tickets.put(ticket, new TicketPayload(
                userId,
                redirectTo,
                LocalDateTime.now().plusSeconds(TICKET_TTL_SECONDS)));
        return ticket;
    }

    public TicketPayload consume(String ticket) {
        cleanupExpiredTickets();

        TicketPayload payload = tickets.remove(ticket);
        if (payload == null || payload.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Google login ticket is invalid or expired");
        }

        return payload;
    }

    private void cleanupExpiredTickets() {
        LocalDateTime now = LocalDateTime.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record TicketPayload(UUID userId, String redirectTo, LocalDateTime expiresAt) {
    }
}
