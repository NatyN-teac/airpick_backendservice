package com.airpick.airpick_service.dtos.output;

import java.util.UUID;

/**
 * Returned to the Flutter app to launch the Veriff SDK / web flow.
 */
public record VerificationSessionResponseDto(
        UUID sessionId,
        String sessionUrl,
        String sessionToken,
        String status
) {}
