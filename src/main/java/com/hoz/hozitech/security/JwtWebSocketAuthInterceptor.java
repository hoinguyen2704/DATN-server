package com.hoz.hozitech.security;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.hoz.hozitech.application.constant.SecurityConstant;
import com.hoz.hozitech.application.repositories.TokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String username = jwtTokenProvider.extractUsername(token);
            if (username == null || username.isBlank()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!userDetails.isEnabled() || !jwtTokenProvider.isTokenValid(token, userDetails) || !isTokenActive(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            if (!(userDetails instanceof CustomUserDetails customUserDetails)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put("userId", customUserDetails.getUser().getId().toString());
            attributes.put("role", customUserDetails.getUser().getRole().getId().name());
            attributes.put("email", customUserDetails.getUser().getEmail());
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
        // No-op
    }

    private String resolveToken(ServerHttpRequest request) {
        String tokenFromQuery = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (tokenFromQuery != null && !tokenFromQuery.isBlank()) {
            return tokenFromQuery.trim();
        }

        String authHeader = request.getHeaders().getFirst(SecurityConstant.HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            return authHeader.substring(SecurityConstant.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }

    private boolean isTokenActive(String jwt) {
        return tokenRepository.findByToken(jwt)
                .filter(token -> !Boolean.TRUE.equals(token.getExpired()))
                .filter(token -> !Boolean.TRUE.equals(token.getRevoked()))
                .filter(token -> token.getExpirationDate() == null || token.getExpirationDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}
