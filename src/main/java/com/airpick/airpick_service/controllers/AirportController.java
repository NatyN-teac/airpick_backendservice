package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.dtos.input.CreateAirportRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateAirportRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.AirportResponseDto;
import com.airpick.airpick_service.services.AirportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes airport management endpoints.
 * <p>
 * Airport creation, update, and deletion are admin operations. The read endpoint
 * is accessible to all authenticated users so they can select airports when
 * creating flights. All responses are wrapped in {@link ApiResponseDto}.
 */
@Tag(
        name = "Airports",
        description = "Admin-managed airport reference data. " +
                      "Airports must be active to be selectable when creating a flight. " +
                      "Create, update, and delete operations are intended for admin use."
)
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    /**
     * Creates a new airport in the system.
     * <p>
     * The IATA code must be unique among active airports.
     *
     * @param request airport details — name, IATA code, country, city
     * @return {@code 201 Created} with the saved airport wrapped in an {@link ApiResponseDto}
     */
    @Operation(
            summary = "Create an airport",
            description = """
                    Creates a new active airport. The IATA code must be unique across \
                    all active airports.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Airport created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "IATA code already in use",
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
    public ResponseEntity<ApiResponseDto<AirportResponseDto>> createAirport(
            @RequestBody CreateAirportRequestDto request) {
        AirportResponseDto airport = airportService.createAirport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(airport));
    }

    /**
     * Updates an existing airport's details.
     * <p>
     * Only non-null fields in the request body are applied.
     *
     * @param airportId the ID of the airport to update
     * @param request   fields to update; null values are ignored
     * @return {@code 200 OK} with the updated airport
     */
    @Operation(
            summary = "Update an airport",
            description = """
                    Updates an existing active airport. Only fields provided in the request \
                    body are applied — omitted or null fields are left unchanged.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Airport updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Airport not found or inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @PatchMapping("/{airportId}")
    public ResponseEntity<ApiResponseDto<AirportResponseDto>> updateAirport(
            @PathVariable UUID airportId,
            @RequestBody UpdateAirportRequestDto request) {
        AirportResponseDto airport = airportService.updateAirport(airportId, request);
        return ResponseEntity.ok(ApiResponseDto.ok(airport));
    }

    /**
     * Soft-deletes an airport by marking it as inactive.
     * <p>
     * The airport record is preserved for historical flight leg references.
     *
     * @param airportId the ID of the airport to deactivate
     * @return {@code 200 OK} with an empty success envelope
     */
    @Operation(
            summary = "Delete an airport",
            description = """
                    Deactivates the specified airport. The record is preserved in the \
                    database for existing flight leg references, but the airport will no \
                    longer appear in listings or be selectable for new flights.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Airport deactivated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Airport not found or already inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))
            )
    })
    @DeleteMapping("/{airportId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteAirport(@PathVariable UUID airportId) {
        airportService.deleteAirport(airportId);
        return ResponseEntity.ok(ApiResponseDto.ok());
    }

    /**
     * Returns all active airports available for flight creation.
     *
     * @return {@code 200 OK} with the list of active airports ordered by name
     */
    @Operation(
            summary = "Get all airports",
            description = """
                    Returns all active airports ordered alphabetically by name. \
                    Use this endpoint to populate airport selection when creating a flight.

                    Requires a valid `Bearer` JWT token in the `Authorization` header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Airports retrieved successfully",
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
    public ResponseEntity<ApiResponseDto<List<AirportResponseDto>>> getAllAirports() {
        List<AirportResponseDto> airports = airportService.getAllAirports();
        return ResponseEntity.ok(ApiResponseDto.ok(airports));
    }
}
