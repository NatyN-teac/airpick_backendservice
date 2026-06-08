package com.airpick.airpick_service.dtos.output;

import java.time.LocalDateTime;
import java.util.UUID;

public record PickupPhotoUrlResponseDto(
        UUID matchId,
        String signedUrl,
        LocalDateTime expiresAt,
        String contentType,
        LocalDateTime uploadedAt
) {}
