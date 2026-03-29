package com.hoz.hozitech.security;

import com.hoz.hozitech.application.repositories.TokenRepository;
import com.hoz.hozitech.application.constant.SecurityConstant;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(SecurityConstant.HEADER_AUTHORIZATION);
        final String jwt;
        final String userEmailOrName;

        // Skip filtering if there's no Bearer token
        if (authHeader == null || !authHeader.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(SecurityConstant.TOKEN_PREFIX.length());

        try {
            userEmailOrName = jwtTokenProvider.extractUsername(jwt);

            // If we have a username and the current security context is not already
            // authenticated
            if (userEmailOrName != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmailOrName);

                // Check if user is deleted/locked
                if (!userDetails.isEnabled()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // If token is valid, set the security context
                if (jwtTokenProvider.isTokenValid(jwt, userDetails) && isTokenActive(jwt)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenActive(String jwt) {
        return tokenRepository.findByToken(jwt)
                .filter(token -> !Boolean.TRUE.equals(token.getExpired()))
                .filter(token -> !Boolean.TRUE.equals(token.getRevoked()))
                .filter(token -> token.getExpirationDate() == null || token.getExpirationDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}
