package com.airpick.airpick_service.dtos.input;

public record CreateCountryRequestDto(
        String name,
        String countryCode
) {}
