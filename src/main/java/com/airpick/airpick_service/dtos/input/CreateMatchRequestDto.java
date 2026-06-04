package com.airpick.airpick_service.dtos.input;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for creating a match request (shipper-initiated).
 * <p>
 * {@code receiverNeeded = false} — the shipper themselves will receive the items.
 * {@code receiverNeeded = true}  — a third-party receiver must be provided via {@code receiver}.
 * Providing {@code receiverNeeded = true} without a populated {@code receiver} is a validation error.
 */
public record CreateMatchRequestDto(
        UUID offerId,
        boolean receiverNeeded,
        ReceiverDto receiver,
        List<MatchItemDto> items
) {
    public record MatchItemDto(
            UUID offerItemId,
            BigDecimal quantity
    ) {}

    public record ReceiverDto(
            String firstName,
            String lastName,
            String phone,
            String photoIdUrl
    ) {}
}
