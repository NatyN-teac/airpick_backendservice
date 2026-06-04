package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.UrgencyLevel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Input DTO for updating a sender's offer request.
 * All fields are optional — only non-null values are applied.
 * sourceCountry and destinationCountry cannot be changed after creation.
 */
public record UpdateSenderOfferRequestDto(
        UUID srcAirportId,
        UUID destAirportId,
        LocalDate preferredDate,
        UrgencyLevel urgencyLevel,
        String specialNote,
        Boolean partialProposalAccepted
) {}
