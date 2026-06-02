package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Airport;

import java.util.UUID;

public record AirportResponseDto(
        UUID id,
        String name,
        String code,
        String country,
        String city
) {
    public static AirportResponseDto from(Airport airport) {
        return new AirportResponseDto(
                airport.getId(),
                airport.getName(),
                airport.getCode(),
                airport.getCountry(),
                airport.getCity()
        );
    }
}
