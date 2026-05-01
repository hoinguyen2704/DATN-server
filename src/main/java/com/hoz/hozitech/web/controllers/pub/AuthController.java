package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.auth.AuthService;
import com.hoz.hozitech.application.services.auth.GoogleLinkIntentTicketService;
import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.config.utils.LocalizationUtils;
import com.hoz.hozitech.domain.dtos.request.LoginRequest;
import com.hoz.hozitech.domain.dtos.request.GoogleTicketExchangeRequest;
import com.hoz.hozitech.domain.dtos.request.RegisterRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.AuthResponse;
import com.hoz.hozitech.domain.dtos.response.GoogleLoginExchangeResponse;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import com.hoz.hozitech.web.base.Authenticated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestAPI("${api.prefix-client}/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String GOOGLE_STATE_COOKIE = "google_oauth_state";
    private static final String GOOGLE_FROM_COOKIE = "google_oauth_from";
    private static final String GOOGLE_LINK_STATE_COOKIE = "google_link_oauth_state";
    private static final String GOOGLE_LINK_FROM_COOKIE = "google_link_oauth_from";
    private static final String GOOGLE_LINK_TICKET_COOKIE = "google_link_oauth_ticket";
    private static final long GOOGLE_COOKIE_TTL_SECONDS = 300;
    private static final String DEFAULT_GOOGLE_LINK_REDIRECT_PATH = "/user/settings";

    private final AuthService authService;
    private final GoogleLinkIntentTicketService googleLinkIntentTicketService;
    private final UserService userService;
    private final LocalizedApiResponseFactory responseFactory;
    private final LocalizationUtils localizationUtils;

    @Value("${social.google.frontend-base-url}")
    private String frontendBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(responseFactory.success("response.auth.registered", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(responseFactory.success("response.auth.login_success", response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidParamException("Refresh token is required")
                    .withMessageKey("error.refresh_token_required");
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(responseFactory.success("response.auth.token_refreshed", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid com.hoz.hozitech.domain.dtos.request.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(responseFactory.success("response.auth.reset_password_otp_sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(@RequestBody @Valid com.hoz.hozitech.domain.dtos.request.VerifyOtpRequest request) {
        boolean isValid = authService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(responseFactory.success("response.auth.otp_verified", isValid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid com.hoz.hozitech.domain.dtos.request.ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(responseFactory.success("response.auth.password_reset"));
    }

    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<AuthResponse>> socialLogin(@RequestBody @Valid com.hoz.hozitech.domain.dtos.request.SocialLoginRequest request) {
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(responseFactory.success("response.auth.social_login_success", response));
    }

    @GetMapping("/google/start")
    public ResponseEntity<Void> startGoogleLogin(
            @RequestParam(required = false) String from) {
        String normalizedFrom = normalizeRedirectPath(from);
        String state = UUID.randomUUID().toString();
        String googleAuthorizationUrl = authService.buildGoogleAuthorizationUrl(state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleAuthorizationUrl)
                .header(HttpHeaders.SET_COOKIE, buildCookie(GOOGLE_STATE_COOKIE, state, GOOGLE_COOKIE_TTL_SECONDS).toString())
                .header(HttpHeaders.SET_COOKIE, buildCookie(GOOGLE_FROM_COOKIE, normalizedFrom, GOOGLE_COOKIE_TTL_SECONDS).toString())
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleGoogleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(name = "error", required = false) String googleError,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = GOOGLE_STATE_COOKIE, required = false) String expectedState,
            @CookieValue(name = GOOGLE_FROM_COOKIE, required = false) String fromCookie) {

        String redirectTo = normalizeRedirectPath(fromCookie);
        ResponseCookie clearStateCookie = clearCookie(GOOGLE_STATE_COOKIE);
        ResponseCookie clearFromCookie = clearCookie(GOOGLE_FROM_COOKIE);

        if (googleError != null && !googleError.isBlank()) {
            return redirectToLoginWithError("GOOGLE_AUTH_ERROR", errorDescription, clearStateCookie, clearFromCookie);
        }

        if (expectedState == null || state == null || !expectedState.equals(state)) {
            return redirectToLoginWithError(
                    "GOOGLE_AUTH_STATE_INVALID",
                    localizationUtils.getLocalizedMessage("error.google_auth_state_invalid"),
                    clearStateCookie,
                    clearFromCookie);
        }

        if (code == null || code.isBlank()) {
            return redirectToLoginWithError(
                    "GOOGLE_AUTH_CODE_MISSING",
                    localizationUtils.getLocalizedMessage("error.google_auth_code_missing"),
                    clearStateCookie,
                    clearFromCookie);
        }

        try {
            String ticket = authService.createGoogleLoginTicketFromAuthorizationCode(code, redirectTo);
            String callbackUrl = buildFrontendUrl("/auth/google/callback", Map.of("ticket", ticket));

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, callbackUrl)
                    .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, clearFromCookie.toString())
                    .build();
        } catch (BusinessException ex) {
            return redirectToLoginWithError(
                    ex.getErrorCode().name(),
                    resolveLocalizedExceptionMessage(ex),
                    clearStateCookie,
                    clearFromCookie);
        } catch (UnauthorizedException ex) {
            return redirectToLoginWithError(
                    "GOOGLE_LOGIN_FAILED",
                    resolveLocalizedExceptionMessage(ex),
                    clearStateCookie,
                    clearFromCookie);
        } catch (Exception ex) {
            return redirectToLoginWithError(
                    "GOOGLE_LOGIN_FAILED",
                    localizationUtils.getLocalizedMessage("error.google_login_failed"),
                    clearStateCookie,
                    clearFromCookie);
        }
    }

    @PostMapping("/google/exchange-ticket")
    public ResponseEntity<ApiResponse<GoogleLoginExchangeResponse>> exchangeGoogleTicket(
            @RequestBody @Valid GoogleTicketExchangeRequest request) {
        GoogleLoginExchangeResponse response = authService.exchangeGoogleLoginTicket(request.getTicket());
        return ResponseEntity.ok(responseFactory.success("response.auth.google_login_success", response));
    }

    @GetMapping("/google/link/start")
    public ResponseEntity<Void> startGoogleLink(
            @RequestParam String ticket,
            @RequestParam(required = false) String from) {
        String normalizedFrom = normalizeGoogleLinkRedirectPath(from);
        if (ticket == null || ticket.isBlank()) {
            return redirectToSettingsWithLinkResult(
                    normalizedFrom,
                    "error",
                    "GOOGLE_LINK_TICKET_MISSING",
                    localizationUtils.getLocalizedMessage("error.google_link_request_invalid"),
                    clearCookie(GOOGLE_LINK_STATE_COOKIE),
                    clearCookie(GOOGLE_LINK_FROM_COOKIE),
                    clearCookie(GOOGLE_LINK_TICKET_COOKIE));
        }

        String state = UUID.randomUUID().toString();
        String googleAuthorizationUrl = authService.buildGoogleAuthorizationUrl(state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleAuthorizationUrl)
                .header(HttpHeaders.SET_COOKIE, buildCookie(GOOGLE_LINK_STATE_COOKIE, state, GOOGLE_COOKIE_TTL_SECONDS).toString())
                .header(HttpHeaders.SET_COOKIE, buildCookie(GOOGLE_LINK_FROM_COOKIE, normalizedFrom, GOOGLE_COOKIE_TTL_SECONDS).toString())
                .header(HttpHeaders.SET_COOKIE, buildCookie(GOOGLE_LINK_TICKET_COOKIE, ticket, GOOGLE_COOKIE_TTL_SECONDS).toString())
                .build();
    }

    @GetMapping("/google/link/callback")
    public ResponseEntity<Void> handleGoogleLinkCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(name = "error", required = false) String googleError,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = GOOGLE_LINK_STATE_COOKIE, required = false) String expectedState,
            @CookieValue(name = GOOGLE_LINK_FROM_COOKIE, required = false) String fromCookie,
            @CookieValue(name = GOOGLE_LINK_TICKET_COOKIE, required = false) String linkTicket) {

        String redirectTo = normalizeGoogleLinkRedirectPath(fromCookie);
        ResponseCookie clearStateCookie = clearCookie(GOOGLE_LINK_STATE_COOKIE);
        ResponseCookie clearFromCookie = clearCookie(GOOGLE_LINK_FROM_COOKIE);
        ResponseCookie clearTicketCookie = clearCookie(GOOGLE_LINK_TICKET_COOKIE);

        if (googleError != null && !googleError.isBlank()) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_AUTH_ERROR",
                    errorDescription,
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        }

        if (expectedState == null || state == null || !expectedState.equals(state)) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_AUTH_STATE_INVALID",
                    localizationUtils.getLocalizedMessage("error.google_link_request_invalid"),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        }

        if (linkTicket == null || linkTicket.isBlank()) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_LINK_TICKET_MISSING",
                    localizationUtils.getLocalizedMessage("error.google_link_request_invalid"),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        }

        if (code == null || code.isBlank()) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_AUTH_CODE_MISSING",
                    localizationUtils.getLocalizedMessage("error.google_link_code_missing"),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        }

        try {
            GoogleLinkIntentTicketService.TicketPayload payload = googleLinkIntentTicketService.consume(linkTicket);
            String idToken = authService.exchangeGoogleAuthorizationCodeForIdToken(code);
            userService.linkGoogleSocialAccountByUserId(payload.userId(), idToken);

            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "success",
                    null,
                    null,
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        } catch (BusinessException ex) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    ex.getErrorCode().name(),
                    resolveLocalizedExceptionMessage(ex),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        } catch (UnauthorizedException ex) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_LINK_FAILED",
                    resolveLocalizedExceptionMessage(ex),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        } catch (Exception ex) {
            return redirectToSettingsWithLinkResult(
                    redirectTo,
                    "error",
                    "GOOGLE_LINK_FAILED",
                    localizationUtils.getLocalizedMessage("error.google_link_failed"),
                    clearStateCookie,
                    clearFromCookie,
                    clearTicketCookie);
        }
    }

    @PostMapping("/logout")
    @Authenticated
    public ResponseEntity<ApiResponse<Void>> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.hoz.hozitech.security.CustomUserDetails userDetails) {
        authService.logout(userDetails.getUser().getId());
        return ResponseEntity.ok(responseFactory.success("response.auth.logout_success"));
    }

    private ResponseEntity<Void> redirectToLoginWithError(
            String code,
            String message,
            ResponseCookie clearStateCookie,
            ResponseCookie clearFromCookie) {
        String loginUrl = buildFrontendUrl("/login", Map.of(
                "google_error_code", code,
                "google_error_message", message == null || message.isBlank()
                        ? localizationUtils.getLocalizedMessage("error.google_login_failed")
                        : message));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, loginUrl)
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearFromCookie.toString())
                .build();
    }

    private ResponseEntity<Void> redirectToSettingsWithLinkResult(
            String redirectPath,
            String status,
            String code,
            String message,
            ResponseCookie clearStateCookie,
            ResponseCookie clearFromCookie,
            ResponseCookie clearTicketCookie) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(normalizeGoogleLinkRedirectPath(redirectPath))
                .queryParam("google_link_status", status);

        if (code != null && !code.isBlank()) {
            builder.queryParam("google_error_code", code);
        }
        if (message != null && !message.isBlank()) {
            builder.queryParam("google_error_message", message);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, builder.build().encode().toUriString())
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearFromCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearTicketCookie.toString())
                .build();
    }

    private String resolveLocalizedExceptionMessage(LocalizedRuntimeException ex) {
        return localizationUtils.resolveLocalizedRuntimeMessage(ex, ex.getMessage());
    }

    private String buildFrontendUrl(String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(path);
        queryParams.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private String normalizeRedirectPath(String from) {
        if (from == null) {
            return "/";
        }

        String value = from.trim();
        if (value.isBlank() || !value.startsWith("/") || value.startsWith("//")) {
            return "/";
        }

        return value;
    }

    private String normalizeGoogleLinkRedirectPath(String from) {
        String normalized = normalizeRedirectPath(from);
        if ("/".equals(normalized)) {
            return DEFAULT_GOOGLE_LINK_REDIRECT_PATH;
        }
        return normalized;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
