package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.CancelMatchRequestDto;
import com.airpick.airpick_service.dtos.input.CreateMatchRequestDto;
import com.airpick.airpick_service.dtos.input.RejectMatchRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateMatchedItemStatusRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.MatchResponseDto;
import com.airpick.airpick_service.dtos.output.MatchTrackResponseDto;
import com.airpick.airpick_service.dtos.output.PickupPhotoUrlResponseDto;
import com.airpick.airpick_service.services.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for match lifecycle management.
 * <p>
 * A shipper initiates a match against an open offer. The carrier then accepts or rejects it.
 * Both parties can cancel within the allowed status window.
 * Delivery progress is tracked at both the match and individual item level.
 */
@Tag(
        name = "Matches",
        description = "Full match lifecycle between shippers and carriers. " +
                      "A shipper creates a match against an open offer specifying which items and quantities they need. " +
                      "The carrier reviews and either accepts or rejects it with a reason. " +
                      "Once accepted, the carrier starts delivery (IN_PROGRESS) and marks it complete. " +
                      "Each matched item has its own delivery status and full audit history."
)
@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    // -------------------------------------------------------------------------
    // Shipper — create match
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create a match request",
            description = "Shipper requests a match against an open offer. " +
                          "Each requested item must belong to the offer and the quantity must not exceed " +
                          "the item's remaining available slots. " +
                          "If all offer slots are filled the offer automatically moves to FULLY_MATCHED. " +
                          "A chat thread is created alongside the match for carrier-shipper communication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Match created successfully, status is PENDING",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Offer not found, offer not OPEN, shipper is the carrier, " +
                                                             "unknown offer item, or requested quantity exceeds available slots",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> createMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateMatchRequestDto request) {
        MatchResponseDto match = matchService.createMatch(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(match));
    }

    // -------------------------------------------------------------------------
    // Carrier — lifecycle transitions
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Accept a match",
            description = "Carrier accepts a PENDING match request. " +
                          "Transitions the match from PENDING to ACCEPTED. " +
                          "The shipper is notified and can now communicate via the attached chat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match accepted, status is now ACCEPTED",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Match is not in PENDING status",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{matchId}/accept")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> acceptMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match to accept") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.acceptMatch(matchId, userDetails.getUsername())));
    }

    @Operation(
            summary = "Reject a match",
            description = "Carrier rejects a PENDING match request. A rejection reason is required. " +
                          "The reserved item quantities are restored to the offer, " +
                          "and if the offer was FULLY_MATCHED it reverts to OPEN. " +
                          "All matched items are set to CANCELLED. " +
                          "Transitions the match from PENDING to REJECTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match rejected, status is now REJECTED",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Match is not PENDING, or rejection reason is missing",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{matchId}/reject")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> rejectMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match to reject") @PathVariable UUID matchId,
            @RequestBody RejectMatchRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.rejectMatch(matchId, userDetails.getUsername(), request)));
    }

    @Operation(
            summary = "Start a match (mark items as collected)",
            description = "Carrier marks an ACCEPTED match as IN_PROGRESS after pickup. " +
                          "Requires a pickup photo to have been uploaded via POST /{matchId}/pickup-photo " +
                          "and all matched items to be in COLLECTED status. " +
                          "Transitions the match from ACCEPTED to IN_PROGRESS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match started, status is now IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Match is not in ACCEPTED status",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{matchId}/start")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> startMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match to start") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.startMatch(matchId, userDetails.getUsername())));
    }

    @Operation(
            summary = "Upload match pickup photo",
            description = "Carrier uploads a pickup proof photo for the match. " +
                          "The image is streamed to a private Google Cloud Storage bucket at " +
                          "matches/{matchId}/pickup-photo.{ext}. " +
                          "All PENDING matched items are marked COLLECTED. " +
                          "Only allowed while the match is ACCEPTED. " +
                          "Required before PATCH /{matchId}/start."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo uploaded and match updated",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Match not found or not owned by carrier")
    })
    @PostMapping(value = "/{matchId}/pickup-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> uploadPickupPhoto(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match") @PathVariable UUID matchId,
            @Parameter(description = "Pickup proof image (JPEG, PNG, or WebP, max 10 MB)")
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                matchService.uploadPickupPhoto(matchId, userDetails.getUsername(), file)));
    }

    @Operation(
            summary = "Get signed URL for pickup photo",
            description = "Returns a short-lived signed URL to view the private pickup photo in GCS. " +
                          "Accessible to the carrier and shipper of the match."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed URL returned",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No pickup photo uploaded"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Match not found or caller is not a participant")
    })
    @GetMapping("/{matchId}/pickup-photo/url")
    public ResponseEntity<ApiResponseDto<PickupPhotoUrlResponseDto>> getPickupPhotoUrl(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                matchService.getPickupPhotoSignedUrl(matchId, userDetails.getUsername())));
    }

    @Operation(
            summary = "Complete a match (mark delivery done)",
            description = "Carrier marks an IN_PROGRESS match as COMPLETED. " +
                          "Any matched items not already in a terminal status are automatically set to DELIVERED. " +
                          "Transitions the match from IN_PROGRESS to COMPLETED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match completed, status is now COMPLETED",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Match is not in IN_PROGRESS status",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{matchId}/complete")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> completeMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match to complete") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.completeMatch(matchId, userDetails.getUsername())));
    }

    // -------------------------------------------------------------------------
    // Either party — cancel
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Cancel a match",
            description = "Cancels a match. Rules differ by role:\n\n" +
                          "- **Shipper**: can cancel only when status is PENDING.\n" +
                          "- **Carrier**: can cancel when status is PENDING or ACCEPTED. " +
                          "Cancelling after ACCEPTED increments the carrier's cancellation count.\n\n" +
                          "Reserved item quantities are restored to the offer. " +
                          "If the offer was FULLY_MATCHED it reverts to OPEN. " +
                          "All matched items are set to CANCELLED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Caller is not permitted to cancel in the match's current status",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found or caller is not a participant",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> cancelMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match to cancel") @PathVariable UUID matchId,
            @RequestBody(required = false) CancelMatchRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.cancelMatch(matchId, userDetails.getUsername(), request)));
    }

    // -------------------------------------------------------------------------
    // Either party — update individual matched item status
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Update a matched item's delivery status",
            description = "Updates the delivery status of a single matched item. " +
                          "Both the carrier and shipper of the match can call this. " +
                          "Typical carrier flow: PENDING → COLLECTED → IN_TRANSIT → DELIVERED. " +
                          "Shipper can flag an issue: DELIVERED → RETURNED or any active status → FAILED. " +
                          "Transitions from terminal statuses (DELIVERED, FAILED, RETURNED, CANCELLED) are blocked. " +
                          "To set items to CANCELLED use the cancel match endpoint instead."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matched item status updated",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Item is in a terminal status, CANCELLED target is not allowed here, " +
                                                             "or the item does not belong to this match",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match or matched item not found, or caller is not a participant",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{matchId}/items/{matchedItemId}")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> updateMatchedItemStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the parent match") @PathVariable UUID matchId,
            @Parameter(description = "ID of the matched item to update") @PathVariable UUID matchedItemId,
            @RequestBody UpdateMatchedItemStatusRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                matchService.updateMatchedItemStatus(matchId, matchedItemId, userDetails.getUsername(), request)));
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get a match by ID",
            description = "Returns full details of a match including all matched items, " +
                          "their delivery status histories, the match status history, and the chat ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> getMatch(
            @Parameter(description = "ID of the match to retrieve") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.getMatch(matchId)));
    }

    @Operation(
            summary = "Get my matches as shipper",
            description = "Returns all matches where the authenticated user is the shipper, " +
                          "across all statuses. Ordered by creation date descending."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me/shipper")
    public ResponseEntity<ApiResponseDto<List<MatchResponseDto>>> getMyMatchesAsShipper(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.getMyMatchesAsShipper(userDetails.getUsername())));
    }

    @Operation(
            summary = "Get my matches as carrier",
            description = "Returns all matches where the authenticated user is the carrier, " +
                          "across all statuses. Ordered by creation date descending."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me/carrier")
    public ResponseEntity<ApiResponseDto<List<MatchResponseDto>>> getMyMatchesAsCarrier(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.getMyMatchesAsCarrier(userDetails.getUsername())));
    }

    @Operation(
            summary = "Track my deliveries as shipper",
            description = "Returns matches where you are the shipper, grouped into three delivery stages:\n\n" +
                          "- **collected** — ACCEPTED (picked up, awaiting or ready to start transit)\n" +
                          "- **inProgress** — IN_PROGRESS (carrier en route)\n" +
                          "- **completed** — COMPLETED (delivered)\n\n" +
                          "Excludes PENDING, CANCELLED, and REJECTED matches. " +
                          "Ordered by most recently updated within each group."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Track summary retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me/track/shipper")
    public ResponseEntity<ApiResponseDto<MatchTrackResponseDto>> getTrackAsShipper(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                matchService.getTrackAsShipper(userDetails.getUsername())));
    }

    @Operation(
            summary = "Track my deliveries as carrier",
            description = "Returns matches where you are the carrier, grouped into three delivery stages:\n\n" +
                          "- **collected** — ACCEPTED (picked up, awaiting or ready to start transit)\n" +
                          "- **inProgress** — IN_PROGRESS (en route to deliver)\n" +
                          "- **completed** — COMPLETED (delivered)\n\n" +
                          "Excludes PENDING, CANCELLED, and REJECTED matches. " +
                          "Ordered by most recently updated within each group."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Track summary retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me/track/carrier")
    public ResponseEntity<ApiResponseDto<MatchTrackResponseDto>> getTrackAsCarrier(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                matchService.getTrackAsCarrier(userDetails.getUsername())));
    }

    @Operation(
            summary = "Get all matches for an offer",
            description = "Returns all matches created against a specific offer. " +
                          "Caller must be the carrier who owns the offer. " +
                          "Useful for a carrier to see all shippers who have matched their offer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Offer not found or not owned by the authenticated carrier",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/offer/{offerId}")
    public ResponseEntity<ApiResponseDto<List<MatchResponseDto>>> getMatchesByOffer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the offer to retrieve matches for") @PathVariable UUID offerId) {
        return ResponseEntity.ok(ApiResponseDto.ok(matchService.getMatchesByOffer(offerId, userDetails.getUsername())));
    }
}
