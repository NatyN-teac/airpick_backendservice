package com.airpick.airpick_service.dtos.output;

import java.time.LocalDateTime;

public record VerificationStatusResponseDto(
        String status,
        boolean isVerified,
        boolean isDenied,
        String message,
        String veriffSessionId,
        LocalDateTime verificationRequestedAt,
        LocalDateTime verifiedAt
) {}
