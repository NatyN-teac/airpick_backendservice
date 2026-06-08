package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.EngagementResponseDto;
import com.airpick.airpick_service.services.EngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Engagement",
        description = "Active user engagement summary based on current mode. " +
                      "CARRIER: pending proposals sent + active matches. " +
                      "SHIPPER: pending proposals received + active matches. " +
                      "Each section returns at most 5 items."
)
@RestController
@RequestMapping("/api/v1/users/engagement")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    @Operation(
            summary = "Get active engagement",
            description = """
                    Returns up to 5 items per section for the user's current mode.

                    **CARRIER mode:**
                    - `proposalsSent` — your pending proposals on shipper requests
                    - `matchedOffers` — active matches on your offers (PENDING, ACCEPTED, IN_PROGRESS)

                    **SHIPPER mode:**
                    - `proposalsReceived` — pending carrier proposals on your requests
                    - `matchedOffers` — your active matches (PENDING, ACCEPTED, IN_PROGRESS)

                    Requires a valid Bearer JWT.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Engagement retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No active mode set"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<EngagementResponseDto>> getEngagement(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        EngagementResponseDto engagement = engagementService.getEngagement(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok(engagement));
    }
}
