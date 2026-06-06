package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.dtos.input.CreateCountryRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.CountryResponseDto;
import com.airpick.airpick_service.services.CountryService;
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

@Tag(
        name = "Countries",
        description = "Supported country list. Used by the frontend to populate country selection " +
                      "when creating offer requests. Admin can add new countries."
)
@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @Operation(
            summary = "Get all supported countries",
            description = "Returns all active countries ordered alphabetically. " +
                          "Use the name and countryCode to populate country pickers in the app. " +
                          "Requires a valid Bearer JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Countries retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<CountryResponseDto>>> getAllCountries() {
        return ResponseEntity.ok(ApiResponseDto.ok(countryService.getAllCountries()));
    }

    @Operation(
            summary = "Add a new country (admin)",
            description = "Admin adds a new supported country. " +
                          "Both name and countryCode must be unique (case-insensitive). " +
                          "countryCode should follow ISO 3166-1 alpha-2 format (e.g. US, GB, NG)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Country created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Country name or code already exists"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<CountryResponseDto>> createCountry(
            @RequestBody CreateCountryRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(countryService.createCountry(request)));
    }
}
