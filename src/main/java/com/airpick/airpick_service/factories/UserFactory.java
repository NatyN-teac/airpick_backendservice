package com.airpick.airpick_service.factories;

import com.airpick.airpick_service.models.Mode;
import com.airpick.airpick_service.models.Role;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;
import org.springframework.stereotype.Component;

/**
 * Factory responsible for constructing {@link User} and {@link UserProfile} entities.
 * <p>
 * Centralises object construction and bidirectional relationship wiring so that
 * services remain focused on business orchestration rather than entity assembly.
 */
@Component
public class UserFactory {

    /**
     * Builds a new {@link User} with an associated {@link UserProfile}, wiring the
     * bidirectional relationship between them before returning.
     * <p>
     * The returned {@link User} is not yet persisted — the caller is responsible
     * for saving it via the repository.
     *
     * @param email       the user's email address extracted from the Firebase token
     * @param providerId  the Firebase UID to store as the provider ID
     * @param picture     the profile picture URL extracted from the Firebase token
     * @param role        the {@link Role} to assign to the user
     * @param activeMode  the initial active {@link Mode} for the user
     * @return a fully wired, unsaved {@link User} entity
     */
    public User buildNewUser(String email, String providerId, String picture, Role role, Mode activeMode) {
        UserProfile profile = UserProfile.builder()
                .profilePictureUrl(picture)
                .build();

        User user = User.builder()
                .email(email)
                .providerId(providerId)
                .role(role)
                .activeMode(activeMode)
                .isActiveUser(true)
                .isBlocked(false)
                .userProfile(profile)
                .build();

        profile.setUser(user);

        return user;
    }
}
