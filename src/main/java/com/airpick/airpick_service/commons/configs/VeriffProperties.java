package com.airpick.airpick_service.commons.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VeriffProperties {

    @Value("${veriff.api-key:}")
    private String apiKey;

    @Value("${veriff.shared-secret:}")
    private String sharedSecret;

    @Value("${veriff.base-url:}")
    private String baseUrl;

    /** Optional deep-link / web callback URL passed to Veriff on session creation. */
    @Value("${veriff.callback-url:}")
    private String callbackUrl;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && sharedSecret != null && !sharedSecret.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    /** Normalized POST /v1/sessions URL derived from the integration base URL. */
    public String sessionsUrl() {
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/v1")) {
            return normalized + "/sessions";
        }
        return normalized + "/v1/sessions";
    }
}
