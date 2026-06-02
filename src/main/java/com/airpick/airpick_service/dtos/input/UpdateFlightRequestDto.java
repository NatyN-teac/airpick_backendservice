package com.airpick.airpick_service.dtos.input;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for updating an existing flight's legs.
 * <p>
 * Replaces all existing legs with the provided list.
 * The number of legs must match the flight's {@code flightType}
 * (1 for ONE_WAY, 2 for ROUND_TRIP).
 */
public record UpdateFlightRequestDto(
        List<LegDto> legs
) {
    public record LegDto(
            UUID srcAirportId,
            UUID destAirportId,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate arrivalDate,
            LocalTime arrivalTime
    ) {}
}
