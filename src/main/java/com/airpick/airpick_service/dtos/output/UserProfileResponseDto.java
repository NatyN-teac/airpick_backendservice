package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.UserProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponseDto(
        UUID userId,
        UUID id,
        String firstName,
        String middleName,
        String lastName,
        boolean isVerified,
        String city,
        String state,
        String country,
        String profilePictureUrl,
        String bio,
        LocalDate dob,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserProfileResponseDto from(UserProfile profile) {
        return new UserProfileResponseDto(
                profile.getUser().getId(),
                profile.getId(),
                profile.getFirstName(),
                profile.getMiddleName(),
                profile.getLastName(),
                profile.isVerified(),
                profile.getCity(),
                profile.getState(),
                profile.getCountry(),
                profile.getProfilePictureUrl(),
                profile.getBio(),
                profile.getDob(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
