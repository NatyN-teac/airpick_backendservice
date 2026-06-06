package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.UrgencyLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


/**
 * Input DTO for a sender creating a new offer request — looking for a carrier to transport items.
 */
public record CreateSenderOfferRequestDto(
        String sourceCountry,
        String sourceCity,
        String destinationCountry,
        LocalDate preferredDate,
        UrgencyLevel urgencyLevel,
        String specialNote,
        boolean partialProposalAccepted,
        List<RequestItemDto> items
) {
    public record RequestItemDto(
            UUID itemId,
            BigDecimal quantity
    ) {}
}
