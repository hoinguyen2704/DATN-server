package com.hoz.hozitech.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHandler;

import com.hoz.hozitech.application.repositories.TokenRepository;
import com.hoz.hozitech.domain.entities.Role;
import com.hoz.hozitech.domain.entities.Token;
import com.hoz.hozitech.domain.enums.RoleType;
import com.hoz.hozitech.domain.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
class JwtWebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenRepository tokenRepository;

    private JwtWebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtWebSocketAuthInterceptor(jwtTokenProvider, userDetailsService, tokenRepository);
    }

    @Test
    void beforeHandshake_shouldAuthenticateUsingQueryToken() {
        String token = "query-token";
        com.hoz.hozitech.domain.entities.User domainUser = buildDomainUser(RoleType.ADMIN);
        CustomUserDetails userDetails = new CustomUserDetails(domainUser);
        ServletServerHttpRequest request = servletRequest("/ws/support?token=" + token, null);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Map<String, Object> attributes = new HashMap<>();

        when(jwtTokenProvider.extractUsername(token)).thenReturn(domainUser.getEmail());
        when(userDetailsService.loadUserByUsername(domainUser.getEmail())).thenReturn(userDetails);
        when(jwtTokenProvider.isTokenValid(token, userDetails)).thenReturn(true);
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(activeToken(token, domainUser)));

        boolean allowed = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), attributes);

        assertTrue(allowed);
        assertEquals(domainUser.getId().toString(), attributes.get("userId"));
        assertEquals("ADMIN", attributes.get("role"));
        assertEquals(domainUser.getEmail(), attributes.get("email"));
    }

    @Test
    void beforeHandshake_shouldAuthenticateUsingAuthorizationHeader() {
        String token = "header-token";
        com.hoz.hozitech.domain.entities.User domainUser = buildDomainUser(RoleType.USER);
        CustomUserDetails userDetails = new CustomUserDetails(domainUser);
        ServletServerHttpRequest request = servletRequest("/ws/support", "Bearer " + token);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Map<String, Object> attributes = new HashMap<>();

        when(jwtTokenProvider.extractUsername(token)).thenReturn(domainUser.getEmail());
        when(userDetailsService.loadUserByUsername(domainUser.getEmail())).thenReturn(userDetails);
        when(jwtTokenProvider.isTokenValid(token, userDetails)).thenReturn(true);
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(activeToken(token, domainUser)));

        boolean allowed = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), attributes);

        assertTrue(allowed);
        assertEquals("USER", attributes.get("role"));
    }

    @Test
    void beforeHandshake_shouldRejectMissingToken() {
        ServletServerHttpRequest request = servletRequest("/ws/support", null);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean allowed = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
    }

    @Test
    void beforeHandshake_shouldRejectInvalidOrInactiveToken() {
        String token = "bad-token";
        com.hoz.hozitech.domain.entities.User domainUser = buildDomainUser(RoleType.USER);
        CustomUserDetails userDetails = new CustomUserDetails(domainUser);
        ServletServerHttpRequest request = servletRequest("/ws/support?token=" + token, null);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        when(jwtTokenProvider.extractUsername(token)).thenReturn(domainUser.getEmail());
        when(userDetailsService.loadUserByUsername(domainUser.getEmail())).thenReturn(userDetails);
        when(jwtTokenProvider.isTokenValid(token, userDetails)).thenReturn(false);

        boolean allowed = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
    }

    @Test
    void beforeHandshake_shouldRejectNonCustomUserDetails() {
        String token = "plain-user-token";
        UserDetails userDetails = User.withUsername("user@example.com").password("secret").authorities("ROLE_USER").build();
        ServletServerHttpRequest request = servletRequest("/ws/support?token=" + token, null);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        when(jwtTokenProvider.extractUsername(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtTokenProvider.isTokenValid(token, userDetails)).thenReturn(true);
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        boolean allowed = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
    }

    private com.hoz.hozitech.domain.entities.User buildDomainUser(RoleType roleType) {
        Role role = new Role(roleType, roleType.getRoleName());
        com.hoz.hozitech.domain.entities.User user = com.hoz.hozitech.domain.entities.User.builder()
                .userName("tester")
                .password("secret")
                .fullName("Realtime Tester")
                .email(roleType.name().toLowerCase() + "@example.com")
                .status(UserStatus.ACTIVE)
                .role(role)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Token activeToken(String tokenValue, com.hoz.hozitech.domain.entities.User user) {
        return Token.builder()
                .token(tokenValue)
                .tokenType("BEARER")
                .expirationDate(LocalDateTime.now().plusMinutes(15))
                .expired(false)
                .revoked(false)
                .user(user)
                .build();
    }

    private ServletServerHttpRequest servletRequest(String uri, String authorizationHeader) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", uri);
        if (authorizationHeader != null) {
            servletRequest.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return new ServletServerHttpRequest(servletRequest);
    }
}
