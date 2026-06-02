package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String email,
        String providerId,
        String role,
        boolean isActiveUser,
        boolean isBlocked,
        LocalDateTime createdAt,
        ProfileDto profile,
        String token
) {

    public record ProfileDto(
            Long id,
            String firstName,
            String middleName,
            String lastName,
            boolean isVerified,
            String city,
            String state,
            String country,
            String profilePictureUrl,
            String bio,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ProfileDto from(UserProfile p) {
            if (p == null) return null;
            return new ProfileDto(
                    p.getId(),
                    p.getFirstName(),
                    p.getMiddleName(),
                    p.getLastName(),
                    p.isVerified(),
                    p.getCity(),
                    p.getState(),
                    p.getCountry(),
                    p.getProfilePictureUrl(),
                    p.getBio(),
                    p.getCreatedAt(),
                    p.getUpdatedAt()
            );
        }
    }

    public static UserResponseDto from(User user, String token) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getProviderId(),
                user.getRole().getName(),
                user.isActiveUser(),
                user.isBlocked(),
                user.getCreatedAt(),
                ProfileDto.from(user.getUserProfile()),
                token
        );
    }
}
