package com.airpick.airpick_service.integrations.veriff;

import com.airpick.airpick_service.commons.configs.VeriffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Low-level HTTP client for Veriff Public API v1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VeriffClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final VeriffProperties veriffProperties;
    private final RestClient restClient;

    /**
     * Creates a Veriff verification session for SDK / web flow.
     * POST /v1/sessions does not require X-HMAC-SIGNATURE on the request.
     */
    public VeriffSessionResult createSession(VeriffSessionRequest request) {
        if (!veriffProperties.isConfigured()) {
            throw new IllegalStateException("Veriff integration is not configured");
        }

        Map<String, Object> body = buildRequestBody(request);
        log.info("Creating Veriff session for endUserId={}", request.endUserId());

        String responseBody = restClient.post()
                .uri(veriffProperties.sessionsUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-AUTH-CLIENT", veriffProperties.getApiKey())
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String errorBody = new String(res.getBody().readAllBytes());
                    log.error("Veriff session creation failed: status={}, body={}", res.getStatusCode(), errorBody);
                    throw new IllegalStateException(
                            "Veriff session creation failed: " + res.getStatusCode() + " — " + errorBody);
                })
                .body(String.class);

        JsonNode response = parseResponse(responseBody);

        if (response == null || !response.has("verification")) {
            throw new IllegalStateException("Unexpected Veriff response: missing verification object");
        }

        JsonNode verification = response.get("verification");
        String sessionId = textOrNull(verification, "id");
        String sessionUrl = textOrNull(verification, "url");
        String sessionToken = textOrNull(verification, "sessionToken");
        String status = textOrNull(verification, "status");

        if (sessionId == null || sessionUrl == null) {
            throw new IllegalStateException("Unexpected Veriff response: missing session id or url");
        }

        log.info("Veriff session created: id={}, status={}", sessionId, status);
        return new VeriffSessionResult(sessionId, sessionUrl, sessionToken, status);
    }

    private Map<String, Object> buildRequestBody(VeriffSessionRequest request) {
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("endUserId", request.endUserId().toString());

        if (veriffProperties.getCallbackUrl() != null && !veriffProperties.getCallbackUrl().isBlank()) {
            verification.put("callback", veriffProperties.getCallbackUrl());
        }

        Map<String, Object> person = new LinkedHashMap<>();
        if (request.firstName() != null && !request.firstName().isBlank()) {
            person.put("firstName", request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            person.put("lastName", request.lastName());
        }
        if (request.dateOfBirth() != null) {
            person.put("dateOfBirth", request.dateOfBirth().toString());
        }
        if (!person.isEmpty()) {
            verification.put("person", person);
        }

        return Map.of("verification", verification);
    }

    private JsonNode parseResponse(String responseBody) {
        try {
            return OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Veriff response", e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
