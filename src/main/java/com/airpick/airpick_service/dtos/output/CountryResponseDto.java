package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Country;

import java.util.UUID;

public record CountryResponseDto(
        UUID id,
        String name,
        String countryCode
) {
    public static CountryResponseDto from(Country country) {
        return new CountryResponseDto(
                country.getId(),
                country.getName(),
                country.getCountryCode()
        );
    }
}
