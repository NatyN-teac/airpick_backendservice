package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Flight;
import com.airpick.airpick_service.models.FlightLeg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record FlightResponseDto(
        UUID id,
        String flightType,
        boolean isBooked,
        List<LegDto> legs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record LegDto(
            UUID id,
            int legOrder,
            AirportResponseDto srcAirport,
            AirportResponseDto destAirport,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate arrivalDate,
            LocalTime arrivalTime
    ) {
        public static LegDto from(FlightLeg leg) {
            return new LegDto(
                    leg.getId(),
                    leg.getLegOrder(),
                    AirportResponseDto.from(leg.getSrcAirport()),
                    AirportResponseDto.from(leg.getDestAirport()),
                    leg.getDepartureDate(),
                    leg.getDepartureTime(),
                    leg.getArrivalDate(),
                    leg.getArrivalTime()
            );
        }
    }

    public static FlightResponseDto from(Flight flight) {
        return new FlightResponseDto(
                flight.getId(),
                flight.getFlightType().name(),
                flight.isBooked(),
                flight.getLegs().stream().map(LegDto::from).toList(),
                flight.getCreatedAt(),
                flight.getUpdatedAt()
        );
    }
}
