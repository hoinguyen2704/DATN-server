package com.hoz.hozitech.application.services.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;

@Service
public class GoogleTokenVerifierService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public GoogleTokenPayload verify(String token) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String issuer = payload.getIssuer();
            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                throw new UnauthorizedException("Invalid Google token issuer");
            }

            Object emailVerifiedObject = payload.get("email_verified");
            boolean emailVerified = emailVerifiedObject instanceof Boolean b ? b
                    : Boolean.parseBoolean(String.valueOf(emailVerifiedObject));
            if (!emailVerified) {
                throw new UnauthorizedException("Google account email is not verified");
            }

            String providerUserId = payload.getSubject();
            String email = payload.getEmail();
            if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank()) {
                throw new UnauthorizedException("Google token does not contain required claims");
            }

            String name = (String) payload.get("name");
            String avatarUrl = (String) payload.get("picture");

            return new GoogleTokenPayload(
                    providerUserId,
                    email.trim().toLowerCase(Locale.ROOT),
                    name,
                    avatarUrl);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Google login failed (" + ex.getMessage() + ")")
                    .withMessageKey("error.google_login_failed_detail", ex.getMessage());
        }
    }

    public record GoogleTokenPayload(String providerUserId, String email, String name, String avatarUrl) {
    }
}
