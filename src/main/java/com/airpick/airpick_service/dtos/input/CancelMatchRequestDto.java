package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for cancelling a match.
 * The optional note explains the reason for cancellation.
 * Applicable to both carrier (PENDING or ACCEPTED) and shipper (PENDING only).
 */
public record CancelMatchRequestDto(
        String note
) {}
