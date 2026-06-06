package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.CreateFlightRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateFlightRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.FlightResponseDto;
import com.airpick.airpick_service.services.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes flight management endpoints for authenticated users.
 * <p>
 * All responses are wrapped in {@link ApiResponseDto} to ensure a consistent
 * response envelope across the API. Errors are handled globally by
 * {@link com.airpick.airpick_service.commons.exceptions.GlobalExceptionHandler}.
 */
@Tag(
        name = "Flights",
        description = "Flight creation, update, removal, and retrieval. " +
                      "A ONE_WAY flight requires exactly one leg; a ROUND_TRIP requires exactly two. " +
                      "Each leg may have independent source and destination airports. " +
                      "All endpoints require a valid Bearer JWT."
)
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    /**
     * Creates a new flight with one or more legs for the authenticated user.
     * <p>
     * ONE_WAY flights require exactly one leg; ROUND_TRIP flights require exactly two.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @param request     flight type and ordered list of leg details
     * @return {@code 201 Created} with the saved flight wrapped in an {@link ApiResponseDto}
     */
    @Operation(
            summary = "Create a flight",
            description = """
                    Creates a new flight for the authenticated user.

                    - **ONE_WAY**: exactly 1 leg required.
                    - **ROUND_TRIP**: exactly 2 legs required. Each leg may have a different \
                    source airport, supporting itineraries where the return departs from a \
                    different city.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Flight created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid leg count for the given flight type, or airport not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<FlightResponseDto>> createFlight(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateFlightRequestDto request) {
        FlightResponseDto flight = flightService.createFlight(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(flight));
    }

    /**
     * Updates the legs of an existing flight owned by the authenticated user.
     * <p>
     * All existing legs are replaced with the provided list. The leg count must
     * match the flight's original type.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @param flightId    the ID of the flight to update
     * @param request     new leg details
     * @return {@code 200 OK} with the updated flight
     */
    @Operation(
            summary = "Update a flight",
            description = """
                    Replaces all legs of an existing flight. The authenticated user must \
                    own the flight. The number of legs provided must match the flight's type \
                    (1 for ONE_WAY, 2 for ROUND_TRIP).

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flight updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid leg count or airport not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Flight not found or does not belong to the authenticated user",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @PutMapping("/{flightId}")
    public ResponseEntity<ApiResponseDto<FlightResponseDto>> updateFlight(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID flightId,
            @RequestBody UpdateFlightRequestDto request) {
        FlightResponseDto flight = flightService.updateFlight(flightId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponseDto.ok(flight));
    }

    /**
     * Soft-deletes a flight owned by the authenticated user.
     * <p>
     * The flight record is preserved in the database with {@code isDeleted = true}.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @param flightId    the ID of the flight to remove
     * @return {@code 200 OK} with an empty success envelope
     */
    @Operation(
            summary = "Remove a flight",
            description = """
                    Soft-deletes the specified flight. The record is preserved for audit \
                    purposes but will no longer appear in any listing or be available for \
                    matching. The authenticated user must own the flight.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flight removed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Flight not found or does not belong to the authenticated user",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @DeleteMapping("/{flightId}")
    public ResponseEntity<ApiResponseDto<Void>> removeFlight(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID flightId) {
        flightService.removeFlight(flightId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok());
    }

    /**
     * Returns all non-deleted flights for the authenticated user.
     *
     * @param userDetails the authenticated user resolved from the JWT
     * @return {@code 200 OK} with the list of flights ordered by creation date descending
     */
    @Operation(
            summary = "Get flights for current user",
            description = """
                    Returns all non-deleted flights belonging to the authenticated user, \
                    ordered by creation date descending.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flights retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<List<FlightResponseDto>>> getAllFlightsByUsername(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<FlightResponseDto> flights = flightService.getAllFlightsByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok(flights));
    }

    /**
     * Returns all non-deleted flights across all users.
     * <p>
     * Admin endpoint — provides a full view of active flight records system-wide.
     *
     * @return {@code 200 OK} with the list of all flights ordered by creation date descending
     */
    @Operation(
            summary = "Get all flights (admin)",
            description = """
                    Returns all non-deleted flights across all users, ordered by creation \
                    date descending. Intended for admin use.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flights retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<FlightResponseDto>>> getAllFlights() {
        List<FlightResponseDto> flights = flightService.getAllFlights();
        return ResponseEntity.ok(ApiResponseDto.ok(flights));
    }
}
