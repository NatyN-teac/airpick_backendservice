package com.airpick.airpick_service.services;

import com.airpick.airpick_service.commons.security.FirebaseTokenService;
import com.airpick.airpick_service.commons.security.JwtUtil;
import com.airpick.airpick_service.dtos.input.UpdateModeRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateUserRequestDto;
import com.airpick.airpick_service.dtos.output.UserResponseDto;
import com.airpick.airpick_service.factories.UserFactory;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.ModeRepository;
import com.airpick.airpick_service.repositories.RoleRepository;
import com.airpick.airpick_service.repositories.UserProfileRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for all user-related business logic including
 * registration, lookup, and account management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final ModeRepository modeRepository;
    private final JwtUtil jwtUtil;
    private final FirebaseTokenService firebaseTokenService;
    private final UserFactory userFactory;

    /**
     * Registers a new user or authenticates a returning user using a Firebase ID token.
     * <p>
     * The token is verified with Firebase Admin SDK to extract the email, provider UID,
     * and profile picture. If the email already exists in the database the user is treated
     * as a returning user: account status is validated (not blocked, is active) and a
     * fresh JWT is issued without creating a new record. If the email is new, a full
     * registration is performed with the {@code CUSTOMER} role and the requested active mode.
     *
     * @param firebaseToken the Firebase ID token sent from the mobile client
     * @param modeType      the initial mode for new users ({@code CARRIER} or {@code SHIPPER});
     *                      ignored for returning users
     * @return a {@link UserResponseDto} containing the user, profile data, and a signed JWT
     * @throws IllegalArgumentException if the token is invalid, the user is blocked,
     *                                  or the user account is inactive
     * @throws IllegalStateException    if the {@code CUSTOMER} role is missing from the database
     */
    @Transactional
    public UserResponseDto register(String firebaseToken, ModeType modeType) {
        log.info("Register/login attempt with mode: {}", modeType);

        FirebaseToken decoded = firebaseTokenService.verify(firebaseToken);
        String email      = decoded.getEmail();
        String providerId = decoded.getUid();
        String picture    = decoded.getPicture();

        log.debug("Firebase token verified for email: {}", email);

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return handleReturningUser(existingUser.get(), email);
        }

        return handleNewUser(email, providerId, picture, modeType);
    }

    /**
     * Updates the profile information of the currently authenticated user.
     * <p>
     * Only non-null fields in the request are applied — null values are ignored,
     * allowing partial updates without overwriting existing data.
     *
     * @param email   the email of the authenticated user, resolved from the JWT
     * @param request the fields to update; any null field is left unchanged
     * @return an updated {@link UserResponseDto} reflecting the new profile state
     * @throws IllegalArgumentException if no user exists for the given email
     */
    @Transactional
    public UserResponseDto updateProfile(String email, UpdateUserRequestDto request) {
        log.info("Updating profile for user: {}", email);

        User user = findByEmail(email);

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.error("Profile not found for user id: {}", user.getId());
                    return new IllegalArgumentException("User not found: " + email);
                });

        if (request.firstName()          != null) profile.setFirstName(request.firstName());
        if (request.middleName()         != null) profile.setMiddleName(request.middleName());
        if (request.lastName()           != null) profile.setLastName(request.lastName());
        if (request.city()               != null) profile.setCity(request.city());
        if (request.state()              != null) profile.setState(request.state());
        if (request.country()            != null) profile.setCountry(request.country());
        if (request.bio()                != null) profile.setBio(request.bio());
        if (request.profilePictureUrl()  != null) profile.setProfilePictureUrl(request.profilePictureUrl());

        userProfileRepository.save(profile);
        log.info("Profile updated for user id: {}", user.getId());

        return UserResponseDto.from(user, null);
    }

    /**
     * Switches the active mode of the currently authenticated user.
     *
     * @param email   the email of the authenticated user, resolved from the JWT
     * @param request contains the new {@link ModeType} to set as active
     * @return an updated {@link UserResponseDto} reflecting the new active mode
     * @throws IllegalArgumentException if the user or mode is not found
     */
    @Transactional
    public UserResponseDto updateMode(String email, UpdateModeRequestDto request) {
        log.info("Updating mode to {} for user: {}", request.mode(), email);

        User user = findByEmail(email);

        Mode newMode = modeRepository.findByName(request.mode().name())
                .orElseThrow(() -> {
                    log.warn("Mode not found: {}", request.mode().name());
                    return new IllegalArgumentException("Mode not found: " + request.mode().name());
                });

        user.setActiveMode(newMode);
        userRepository.save(user);
        log.info("Mode updated to {} for user id: {}", request.mode(), user.getId());

        return UserResponseDto.from(user, null);
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address to search for
     * @return the matching {@link User} entity
     * @throws IllegalArgumentException if no user exists with the given email
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new IllegalArgumentException("User not found: " + email);
                });
    }

    /**
     * Retrieves a user by their internal database ID.
     *
     * @param id the primary key of the user
     * @return the matching {@link User} entity
     * @throws IllegalArgumentException if no user exists with the given ID
     */
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for id: {}", id);
                    return new IllegalArgumentException("User not found: " + id);
                });
    }

    /**
     * Validates a returning user's account status and issues a fresh JWT.
     *
     * @param user  the existing {@link User} record matched by email
     * @param email the email extracted from the Firebase token
     * @return a {@link UserResponseDto} with a newly generated JWT
     * @throws IllegalArgumentException if the account is blocked or inactive
     */
    private UserResponseDto handleReturningUser(User user, String email) {
        log.info("Returning user detected for email: {}", email);

        if (user.isBlocked()) {
            log.warn("Login attempt by blocked user: {}", email);
            throw new IllegalArgumentException("Account is blocked: " + email);
        }

        if (!user.isActiveUser()) {
            log.warn("Login attempt by inactive user: {}", email);
            throw new IllegalArgumentException("Account is inactive: " + email);
        }

        String token = jwtUtil.generateToken(email);
        log.info("JWT issued for returning user id: {}", user.getId());

        return UserResponseDto.from(user, token);
    }

    /**
     * Performs full registration of a brand-new user.
     * <p>
     * Resolves the {@code CUSTOMER} role and the requested mode from the database,
     * delegates entity construction to {@link UserFactory}, persists the result,
     * and issues a signed JWT.
     *
     * @param email      email extracted from the Firebase token
     * @param providerId Firebase UID to store as the provider ID
     * @param picture    profile picture URL from the Firebase token
     * @param modeType   the initial active mode for the new user
     * @return a {@link UserResponseDto} with a newly generated JWT
     * @throws IllegalArgumentException if the requested mode does not exist
     * @throws IllegalStateException    if the {@code CUSTOMER} role is missing from the database
     */
    private UserResponseDto handleNewUser(String email, String providerId, String picture, ModeType modeType) {
        log.info("New user registration for email: {}", email);

        Role customerRole = roleRepository.findByName(RoleType.CUSTOMER.name())
                .orElseThrow(() -> {
                    log.error("CUSTOMER role not found in database");
                    return new IllegalStateException("CUSTOMER role not found");
                });

        Mode activeMode = modeRepository.findByName(modeType.name())
                .orElseThrow(() -> {
                    log.warn("Mode not found: {}", modeType.name());
                    return new IllegalArgumentException("Mode not found: " + modeType.name());
                });

        User user = userFactory.buildNewUser(email, providerId, picture, customerRole, activeMode);
        User saved = userRepository.save(user);
        log.info("New user registered successfully with id: {}", saved.getId());

        String token = jwtUtil.generateToken(email);
        return UserResponseDto.from(saved, token);
    }
}
