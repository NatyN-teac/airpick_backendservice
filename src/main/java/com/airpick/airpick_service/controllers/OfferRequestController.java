package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.CreateSenderOfferRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateSenderOfferRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.SenderOfferRequestResponseDto;
import com.airpick.airpick_service.services.OfferRequestService;
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
 * REST controller for sender offer request lifecycle management.
 * <p>
 * Senders (shippers) create offer requests to broadcast their shipping needs to carriers.
 * Carriers browse open requests and submit proposals with their price.
 * All endpoints require a valid JWT.
 */
@Tag(
        name = "Offer Requests",
        description = "Sender-initiated offer request management. " +
                      "A sender creates a request specifying what items they need shipped, origin/destination, " +
                      "and whether partial carrier proposals are accepted. " +
                      "Carriers browse open requests via /browse (own requests excluded) and respond with proposals. " +
                      "When the sender accepts a proposal, an Offer and Match are created atomically."
)
@RestController
@RequestMapping("/offer-requests")
@RequiredArgsConstructor
public class OfferRequestController {

    private final OfferRequestService offerRequestService;

    // -------------------------------------------------------------------------
    // Sender mutation endpoints
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create an offer request",
            description = "Sender creates a new offer request advertising items they want shipped. " +
                          "sourceCountry and destinationCountry are required. " +
                          "srcAirportId and destAirportId are optional refinements. " +
                          "At least one item must be provided. " +
                          "If partialProposalAccepted is false (default), carriers must price every item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offer request created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<SenderOfferRequestResponseDto>> createRequest(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody CreateSenderOfferRequestDto dto) {

        SenderOfferRequestResponseDto response = offerRequestService.createRequest(principal.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(response));
    }

    @Operation(
            summary = "Update an offer request",
            description = "Sender updates their own offer request. " +
                          "Only non-null fields are applied. " +
                          "sourceCountry and destinationCountry cannot be changed. " +
                          "Cannot update an ACCEPTED or CANCELLED request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer request updated"),
            @ApiResponse(responseCode = "400", description = "Invalid state or input"),
            @ApiResponse(responseCode = "403", description = "Request belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SenderOfferRequestResponseDto>> updateRequest(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Offer request ID") @PathVariable UUID id,
            @RequestBody UpdateSenderOfferRequestDto dto) {

        SenderOfferRequestResponseDto response = offerRequestService.updateRequest(principal.getUsername(), id, dto);
        return ResponseEntity.ok(ApiResponseDto.ok(response));
    }

    @Operation(
            summary = "Cancel an offer request",
            description = "Sender cancels their own offer request. " +
                          "Only OPEN or PROPOSAL_RECEIVED requests can be cancelled. " +
                          "Pending proposals on the request are not automatically rejected here — " +
                          "they will see the request as CANCELLED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Offer request cancelled"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel in current state"),
            @ApiResponse(responseCode = "403", description = "Request belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRequest(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Offer request ID") @PathVariable UUID id) {

        offerRequestService.cancelRequest(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get offer request by ID",
            description = "Fetch a single offer request by its ID. Available to any authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer request found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<SenderOfferRequestResponseDto>> getRequest(
            @Parameter(description = "Offer request ID") @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponseDto.ok(offerRequestService.getRequest(id)));
    }

    @Operation(
            summary = "Get my offer requests",
            description = "Returns all offer requests created by the authenticated sender, in all statuses."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<List<SenderOfferRequestResponseDto>>> getMyRequests(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(ApiResponseDto.ok(offerRequestService.getMyRequests(principal.getUsername())));
    }

    @Operation(
            summary = "Get all open offer requests",
            description = "Returns all OPEN offer requests across all senders. Includes own requests."
    )
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SenderOfferRequestResponseDto>>> getAllOpenRequests() {
        return ResponseEntity.ok(ApiResponseDto.ok(offerRequestService.getAllOpenRequests()));
    }

    @Operation(
            summary = "Browse open offer requests",
            description = "Returns all OPEN offer requests excluding the authenticated user's own. " +
                          "Carriers use this to discover requests to bid on."
    )
    @GetMapping("/browse")
    public ResponseEntity<ApiResponseDto<List<SenderOfferRequestResponseDto>>> browseRequests(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(ApiResponseDto.ok(offerRequestService.browseRequests(principal.getUsername())));
    }
}
