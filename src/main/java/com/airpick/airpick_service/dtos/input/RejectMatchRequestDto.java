package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for rejecting a pending match.
 * The carrier must provide a rejection reason so the shipper understands why.
 */
public record RejectMatchRequestDto(
        String rejectionReason
) {}
