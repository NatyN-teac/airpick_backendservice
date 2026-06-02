package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.FlightType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for creating a new flight.
 * <p>
 * For {@code ONE_WAY} flights supply exactly one leg.
 * For {@code ROUND_TRIP} flights supply exactly two legs in order:
 * leg 1 = outbound, leg 2 = return.
 */
public record CreateFlightRequestDto(
        FlightType flightType,
        List<LegDto> legs
) {
    /**
     * A single flight segment. {@code srcAirportId} and {@code destAirportId}
     * must reference active airports from the airports table.
     */
    public record LegDto(
            UUID srcAirportId,
            UUID destAirportId,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate arrivalDate,
            LocalTime arrivalTime
    ) {}
}
