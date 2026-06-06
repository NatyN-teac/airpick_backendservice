package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.RegisterDeviceRequestDto;
import com.airpick.airpick_service.dtos.input.RegisterRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateModeRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateUserRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.UserProfileResponseDto;
import com.airpick.airpick_service.dtos.output.UserResponseDto;
import com.airpick.airpick_service.services.UserDeviceService;
import com.airpick.airpick_service.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller that exposes user-related API endpoints.
 * <p>
 * All responses are wrapped in {@link ApiResponseDto} to ensure a consistent
 * response envelope across the API. Errors are handled globally by
 * {@link com.airpick.airpick_service.commons.exceptions.GlobalExceptionHandler}.
 */
@Tag(
        name = "Users",
        description = "User registration and account management. " +
                      "Registration is Firebase-based — the mobile client authenticates with Firebase " +
                      "and sends the resulting ID token. The server verifies it, creates or retrieves " +
                      "the user, and returns a signed JWT for all subsequent requests."
)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDeviceService userDeviceService;

    /**
     * Registers a new user account using a Firebase ID token from the mobile client.
     * <p>
     * The token is verified server-side via Firebase Admin SDK. On success, a new user
     * is created with the {@code CUSTOMER} role and the selected initial mode.
     * If the email already exists the account status is validated and a fresh JWT is
     * returned instead. This endpoint is publicly accessible and does not require a bearer token.
     *
     * @param request contains the Firebase ID token and the desired initial mode
     *                ({@code CARRIER} or {@code SHIPPER})
     * @return {@code 201 Created} with the registered user and profile data wrapped
     *         in an {@link ApiResponseDto}
     */
    @Operation(
            summary = "Register or sign in via Firebase",
            description = """
                    Accepts a Firebase ID token issued by the mobile client after the user \
                    authenticates with Firebase (Google, Apple, email, etc.).

                    **New user:** verifies the token, creates a user record with the CUSTOMER role \
                    and the selected mode (CARRIER = traveler, SHIPPER = sender), and returns the \
                    full profile along with a signed JWT.

                    **Returning user:** verifies the token, confirms the account is active and not \
                    blocked, and returns the existing profile with a fresh JWT.

                    The returned JWT must be included as a `Bearer` token in the `Authorization` \
                    header for all authenticated endpoints.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered or signed in successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Firebase token is invalid or expired",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Account is blocked or inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requested mode does not exist",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> register(@RequestBody RegisterRequestDto request) {
        UserResponseDto user = userService.register(request.firebaseToken(), request.mode());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(user));
    }

    /**
     * Updates the profile information of the currently authenticated user.
     * <p>
     * Only non-null fields in the request body are applied — omitted or null fields
     * are left unchanged, allowing partial updates.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @param request     the profile fields to update
     * @return {@code 200 OK} with the updated user and profile data
     */
    @Operation(
            summary = "Update profile information",
            description = """
                    Updates the profile of the currently authenticated user. \
                    Only the fields provided in the request body are updated — \
                    any field set to null or omitted is left unchanged.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User or profile not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @PutMapping("/update")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateUserRequestDto request) {
        UserResponseDto user = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponseDto.ok(user));
    }

    @Operation(
            summary = "Get user profile by ID",
            description = """
                    Returns the public profile details for the user with the given ID. \
                    Intended for viewing carrier or shipper profiles in the marketplace \
                    (e.g. from a match or offer).

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User or profile not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> getProfileByUserId(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        UserProfileResponseDto profile = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.ok(profile));
    }

    /**
     * Switches the active mode of the currently authenticated user.
     * <p>
     * A user can operate as either a {@code CARRIER} (traveler carrying items)
     * or a {@code SHIPPER} (sender dispatching items) and may switch between
     * the two at any time.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @param request     contains the new mode to set
     * @return {@code 200 OK} with the updated user reflecting the new active mode
     */
    @Operation(
            summary = "Switch active mode",
            description = """
                    Switches the authenticated user's active mode between \
                    `CARRIER` (traveler carrying items) and `SHIPPER` (sender dispatching items). \
                    The selected mode is stored as `activeMode` on the user record.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mode updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or unrecognised mode value",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @PatchMapping("/update-mode")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateMode(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateModeRequestDto request) {
        UserResponseDto user = userService.updateMode(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponseDto.ok(user));
    }

    // -------------------------------------------------------------------------
    // Device management
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Register or refresh FCM device token",
            description = """
                    Registers the device's FCM token for push notification delivery. \
                    Flutter must call this endpoint on every app start and whenever \
                    FirebaseMessaging.instance.onTokenRefresh fires.

                    If the token already exists it is refreshed (lastActive updated). \
                    A user can have multiple devices registered simultaneously — \
                    all will receive push notifications.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device registered or refreshed"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/devices")
    public ResponseEntity<Void> registerDevice(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody RegisterDeviceRequestDto request) {
        userDeviceService.registerDevice(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unregister FCM device token",
            description = """
                    Removes the device's FCM token on logout so the device stops receiving \
                    push notifications. Flutter should call this before signing out.

                    Pass the FCM token as a request parameter: `?fcmToken=<token>`.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device unregistered"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @DeleteMapping("/devices")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String fcmToken) {
        userDeviceService.unregisterDevice(userDetails.getUsername(), fcmToken);
        return ResponseEntity.noContent().build();
    }
}
