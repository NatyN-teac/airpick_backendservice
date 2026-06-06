package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.FlightType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for a carrier submitting a price proposal on a sender's offer request.
 * <p>
 * Flight details are embedded directly — the backend creates the flight and the
 * proposal atomically in a single transaction. No pre-existing flightId is needed.
 * <p>
 * {@code items} must cover all request items unless the request has {@code partialProposalAccepted = true}.
 * {@code deliveryArea} and {@code pickupArea} are optional — they fall back to
 * the request's destinationCountry and sourceCountry respectively when null.
 * {@code meetupPlaces} and {@code paymentMethods} are required.
 */
public record SubmitProposalRequestDto(
        FlightDto flight,
        String deliveryArea,
        String pickupArea,
        BigDecimal discount,
        List<String> meetupPlaces,
        List<String> paymentMethods,
        String note,
        List<ProposalItemDto> items
) {
    public record FlightDto(
            FlightType flightType,
            List<LegDto> legs
    ) {}

    public record LegDto(
            UUID srcAirportId,
            UUID destAirportId,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate arrivalDate,
            LocalTime arrivalTime
    ) {}

    public record ProposalItemDto(
            UUID offerRequestItemId,
            BigDecimal pricePerItem
    ) {}
}
