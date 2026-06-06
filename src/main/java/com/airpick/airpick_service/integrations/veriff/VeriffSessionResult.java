package com.airpick.airpick_service.integrations.veriff;

public record VeriffSessionResult(
        String sessionId,
        String sessionUrl,
        String sessionToken,
        String status
) {}
