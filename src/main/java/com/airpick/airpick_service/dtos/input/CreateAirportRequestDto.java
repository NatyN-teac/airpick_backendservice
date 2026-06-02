package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for creating a new airport (admin only).
 */
public record CreateAirportRequestDto(
        String name,
        String code,
        String country,
        String city
) {}
