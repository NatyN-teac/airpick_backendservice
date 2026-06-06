package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.CreateOfferRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateOfferRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.OfferResponseDto;
import com.airpick.airpick_service.services.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller for offer lifecycle management.
 * <p>
 * Carriers create and manage their own offers.
 * Shippers browse offers posted by other carriers.
 * All endpoints require a valid JWT.
 */
@Tag(
        name = "Offers",
        description = "Carrier-initiated offer management and shipper browsing. " +
                      "A carrier creates an offer linked to their flight, specifying items they can carry, " +
                      "prices, pickup/delivery areas, and accepted payment methods. " +
                      "Shippers use the /browse endpoints to discover offers they can match with " +
                      "(own offers are always excluded from browse results)."
)
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    // -------------------------------------------------------------------------
    // Carrier mutation endpoints
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create an offer",
            description = "Carrier creates a new DIRECT offer linked to one of their flights. " +
                          "Each flight can have at most one active DIRECT offer at a time. " +
                          "At least one item must be provided. Each item's remainingQuantity is " +
                          "initialised to its quantity at creation time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offer created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid flight, unknown item, or a duplicate active offer already exists for this flight",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> createOffer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateOfferRequestDto request) {
        OfferResponseDto offer = offerService.createOffer(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(offer));
    }

    @Operation(
            summary = "Update an offer",
            description = "Partially updates non-item fields of an offer owned by the authenticated carrier. " +
                          "Only non-null fields are applied — omit a field to leave it unchanged. " +
                          "Offer items are managed separately and cannot be updated here. " +
                          "Updating is not allowed when the offer status is CANCELLED or FULLY_MATCHED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Offer is in a terminal status (CANCELLED or FULLY_MATCHED) and cannot be updated",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Offer not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{offerId}")
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> updateOffer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the offer to update") @PathVariable UUID offerId,
            @RequestBody UpdateOfferRequestDto request) {
        OfferResponseDto offer = offerService.updateOffer(offerId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponseDto.ok(offer));
    }

    @Operation(
            summary = "Cancel an offer",
            description = "Cancels an OPEN offer owned by the authenticated carrier. " +
                          "Only offers in OPEN status can be cancelled. " +
                          "Offers in any other status (FULLY_MATCHED, CLOSED, already CANCELLED) will be rejected."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Offer is not in OPEN status and cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Offer not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{offerId}")
    public ResponseEntity<ApiResponseDto<Void>> cancelOffer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the offer to cancel") @PathVariable UUID offerId) {
        offerService.cancelOffer(offerId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok());
    }

    // -------------------------------------------------------------------------
    // Single offer retrieval
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get an offer by ID",
            description = "Returns full details of a single offer, including all offer items and flight info."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{offerId}")
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> getOffer(
            @Parameter(description = "ID of the offer to retrieve") @PathVariable UUID offerId) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getOffer(offerId)));
    }

    // -------------------------------------------------------------------------
    // My offers (carrier dashboard)
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get my offers",
            description = "Returns all offers created by the authenticated carrier across all statuses " +
                          "(OPEN, FULLY_MATCHED, CLOSED, CANCELLED). Ordered by creation date descending."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> getMyOffers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getMyOffers(userDetails.getUsername())));
    }

    // -------------------------------------------------------------------------
    // OPEN offers
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all open offers",
            description = "Returns all offers in OPEN status across all carriers. " +
                          "Includes the authenticated user's own offers. " +
                          "Use /offers/browse to exclude your own."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> getAllOpenOffers() {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getAllOpenOffers()));
    }

    @Operation(
            summary = "Browse open offers (shipper view)",
            description = "Returns all OPEN offers excluding those created by the authenticated user. " +
                          "This is the primary endpoint for shippers browsing available offers — " +
                          "a user cannot match with an offer they created as a carrier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/browse")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> browseOffers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.browseOffers(userDetails.getUsername())));
    }

    // -------------------------------------------------------------------------
    // CLOSED offers
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all closed offers",
            description = "Returns all offers in CLOSED status across all carriers. " +
                          "A closed offer was manually closed by the carrier and is no longer accepting matches."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/closed")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> getAllClosedOffers() {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getAllClosedOffers()));
    }

    @Operation(
            summary = "Browse closed offers (excluding mine)",
            description = "Returns all CLOSED offers excluding those created by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/closed/browse")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> browseClosedOffers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.browseClosedOffers(userDetails.getUsername())));
    }

    // -------------------------------------------------------------------------
    // FULLY_MATCHED offers
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all fully matched offers",
            description = "Returns all offers in FULLY_MATCHED status across all carriers. " +
                          "A fully matched offer has had all its item slots filled — " +
                          "remainingQuantity is 0 for every offer item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/fully-matched")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> getAllFullyMatchedOffers() {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getAllFullyMatchedOffers()));
    }

    @Operation(
            summary = "Browse fully matched offers (excluding mine)",
            description = "Returns all FULLY_MATCHED offers excluding those created by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/fully-matched/browse")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> browseFullyMatchedOffers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.browseFullyMatchedOffers(userDetails.getUsername())));
    }

    // -------------------------------------------------------------------------
    // CANCELLED offers
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all cancelled offers",
            description = "Returns all offers in CANCELLED status across all carriers. " +
                          "Cancelled offers were withdrawn by the carrier before any matches were completed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/cancelled")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> getAllCancelledOffers() {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.getAllCancelledOffers()));
    }

    @Operation(
            summary = "Browse cancelled offers (excluding mine)",
            description = "Returns all CANCELLED offers excluding those created by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/cancelled/browse")
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> browseCancelledOffers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(offerService.browseCancelledOffers(userDetails.getUsername())));
    }
}
