package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for updating an airport (admin only).
 * All fields are optional — only non-null values are applied.
 */
public record UpdateAirportRequestDto(
        String name,
        String code,
        String country,
        String city
) {}
