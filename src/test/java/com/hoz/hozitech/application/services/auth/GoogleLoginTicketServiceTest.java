package com.hoz.hozitech.application.services.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoogleLoginTicketServiceTest {

    @Test
    void issuedTicketCanBeConsumedOnlyOnce() {
        GoogleLoginTicketService service = new GoogleLoginTicketService();
        UUID userId = UUID.randomUUID();

        String ticket = service.issue(userId, "/checkout");
        GoogleLoginTicketService.TicketPayload payload = service.consume(ticket);

        assertEquals(userId, payload.userId());
        assertEquals("/checkout", payload.redirectTo());
        assertThrows(UnauthorizedException.class, () -> service.consume(ticket));
    }
}
