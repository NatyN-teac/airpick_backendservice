package com.airpick.airpick_service.commons.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Infrastructure component responsible for all Firebase token operations.
 * <p>
 * Wraps the Firebase Admin SDK to keep Firebase-specific logic out of
 * business services. Any future Firebase operations (custom claims,
 * token revocation checks, etc.) should be added here.
 */
@Slf4j
@Component
public class FirebaseTokenService {

    /**
     * Verifies a Firebase ID token using the Firebase Admin SDK and returns
     * the decoded token containing the user's claims.
     *
     * @param token the raw Firebase ID token sent from the mobile client
     * @return the decoded {@link FirebaseToken}
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    public FirebaseToken verify(String token) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(token);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired Firebase token", e);
        }
    }

    /**
     * Extracts the sign-in provider (e.g. {@code google.com}, {@code apple.com})
     * from the {@code firebase} claim inside a decoded Firebase token.
     *
     * @param decoded a previously verified {@link FirebaseToken}
     * @return the sign-in provider string, or {@code null} if the claim is absent
     */
    @SuppressWarnings("unchecked")
    public String extractProvider(FirebaseToken decoded) {
        Map<String, Object> firebaseClaim =
                (Map<String, Object>) decoded.getClaims().get("firebase");
        if (firebaseClaim == null) return null;
        return (String) firebaseClaim.get("sign_in_provider");
    }
}
