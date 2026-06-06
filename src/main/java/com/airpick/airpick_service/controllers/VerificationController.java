package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.VerificationSessionResponseDto;
import com.airpick.airpick_service.dtos.output.VerificationStatusResponseDto;
import com.airpick.airpick_service.services.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Identity Verification",
        description = "Veriff identity verification for user trust badges. " +
                      "The mobile app calls create-session, launches the Veriff Flutter SDK with the returned URL, " +
                      "and polls status until the decision webhook updates the account."
)
@RestController
@RequestMapping("/api/v1/users/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final IdentityVerificationService identityVerificationService;

    @Operation(
            summary = "Start Veriff verification session",
            description = """
                    Creates a Veriff verification session for the authenticated user and returns the \
                    session URL and token for the Flutter Veriff SDK.

                    Pre-fills person data from the user's profile when available (name, date of birth).

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veriff session created",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "User is already verified"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "500", description = "Veriff integration error")
    })
    @PostMapping("/session")
    public ResponseEntity<ApiResponseDto<VerificationSessionResponseDto>> createSession(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Mobile verification session request received for user: {}", userDetails.getUsername());
        VerificationSessionResponseDto session =
                identityVerificationService.createSession(userDetails.getUsername());
        log.info("Mobile verification session created for user: {}, sessionId: {}",
                userDetails.getUsername(), session.sessionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(session));
    }

    @Operation(
            summary = "Get verification status",
            description = """
                    Returns the current Veriff verification status for the authenticated user.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponseDto<VerificationStatusResponseDto>> getStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        VerificationStatusResponseDto status =
                identityVerificationService.getStatus(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok(status));
    }
}
