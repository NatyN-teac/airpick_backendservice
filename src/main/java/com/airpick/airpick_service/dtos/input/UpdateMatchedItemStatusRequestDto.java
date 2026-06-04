package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.MatchedItemStatus;

/**
 * Input DTO for updating the delivery status of a single matched item.
 * Used by carriers to advance the item through its delivery lifecycle
 * (e.g. PENDING → COLLECTED → IN_TRANSIT → DELIVERED)
 * and by shippers to flag issues (e.g. DELIVERED → RETURNED).
 */
public record UpdateMatchedItemStatusRequestDto(
        MatchedItemStatus status,
        String note
) {}
