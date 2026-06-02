package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for updating a user's profile information.
 * <p>
 * All fields are optional — only non-null values are applied.
 * Send only the fields you want to change.
 */
public record UpdateUserRequestDto(
        String firstName,
        String middleName,
        String lastName,
        String city,
        String state,
        String country,
        String bio,
        String profilePictureUrl
) {}
