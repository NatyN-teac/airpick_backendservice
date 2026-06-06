package com.airpick.airpick_service.commons.security;

import com.airpick.airpick_service.commons.configs.VeriffProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC-SHA256 helpers for Veriff API responses and webhook signature validation.
 */
@Component
@RequiredArgsConstructor
public class VeriffHmacService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final VeriffProperties veriffProperties;

    public String sign(String payload) {
        return hmacSha256Hex(veriffProperties.getSharedSecret(), payload);
    }

    public boolean isValidWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String expected = sign(rawBody);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute Veriff HMAC signature", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
