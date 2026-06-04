package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.AcceptProposalRequestDto;
import com.airpick.airpick_service.dtos.input.SubmitProposalRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.MatchResponseDto;
import com.airpick.airpick_service.dtos.output.OfferProposalResponseDto;
import com.airpick.airpick_service.services.OfferProposalService;
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
 * REST controller for offer proposal lifecycle management.
 * <p>
 * Carriers submit proposals on open sender requests. Senders can reject individual
 * proposals or accept one — acceptance atomically creates an Offer, a Match, and a Chat.
 * All endpoints require a valid JWT.
 */
@Tag(
        name = "Offer Proposals",
        description = "Carrier proposal management for sender offer requests. " +
                      "A carrier submits a proposal with per-item prices for a sender's open request. " +
                      "The sender can reject or accept proposals. Accepting one proposal atomically " +
                      "rejects all others, creates an Offer (FROM_PROPOSAL / FULLY_MATCHED), " +
                      "creates a Match (ACCEPTED), and opens a Chat between both parties."
)
@RestController
@RequiredArgsConstructor
public class OfferProposalController {

    private final OfferProposalService offerProposalService;

    // -------------------------------------------------------------------------
    // Carrier — submit / withdraw
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Submit a proposal",
            description = "Carrier submits a price proposal for a sender's open offer request. " +
                          "The carrier must price all items unless the request has partialProposalAccepted=true. " +
                          "A carrier may only have one active (PENDING/ACCEPTED) proposal per request at a time. " +
                          "meetupPlaces and paymentMethods are required. " +
                          "deliveryArea and pickupArea are optional — they fall back to the request's " +
                          "destinationCountry and sourceCountry respectively."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposal submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or partial proposal not allowed"),
            @ApiResponse(responseCode = "404", description = "Offer request not found"),
            @ApiResponse(responseCode = "409", description = "Carrier already has an active proposal on this request")
    })
    @PostMapping("/offer-requests/{requestId}/proposals")
    public ResponseEntity<ApiResponseDto<OfferProposalResponseDto>> submitProposal(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Offer request ID") @PathVariable UUID requestId,
            @RequestBody SubmitProposalRequestDto dto) {

        OfferProposalResponseDto response = offerProposalService.submitProposal(principal.getUsername(), requestId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(response));
    }

    @Operation(
            summary = "Withdraw a proposal",
            description = "Carrier withdraws their own PENDING proposal. A withdrawn proposal cannot be reinstated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proposal withdrawn"),
            @ApiResponse(responseCode = "400", description = "Proposal is not in PENDING status"),
            @ApiResponse(responseCode = "403", description = "Proposal belongs to another carrier"),
            @ApiResponse(responseCode = "404", description = "Proposal not found")
    })
    @DeleteMapping("/offer-requests/proposals/{proposalId}/withdraw")
    public ResponseEntity<Void> withdrawProposal(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Proposal ID") @PathVariable UUID proposalId) {

        offerProposalService.withdrawProposal(principal.getUsername(), proposalId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Sender — reject / accept
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Reject a proposal",
            description = "Sender rejects a PENDING carrier proposal on their request. " +
                          "Only the sender who owns the request can reject proposals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proposal rejected"),
            @ApiResponse(responseCode = "400", description = "Proposal is not in PENDING status"),
            @ApiResponse(responseCode = "403", description = "Request belongs to another sender"),
            @ApiResponse(responseCode = "404", description = "Proposal not found")
    })
    @PatchMapping("/offer-requests/proposals/{proposalId}/reject")
    public ResponseEntity<Void> rejectProposal(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Proposal ID") @PathVariable UUID proposalId) {

        offerProposalService.rejectProposal(principal.getUsername(), proposalId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Accept a proposal",
            description = "Sender accepts a PENDING carrier proposal. This is an atomic operation that:\n" +
                          "1. Marks this proposal as ACCEPTED.\n" +
                          "2. Marks all other PENDING proposals on the same request as REJECTED.\n" +
                          "3. Marks the offer request as ACCEPTED.\n" +
                          "4. Creates an Offer with source=FROM_PROPOSAL and status=FULLY_MATCHED.\n" +
                          "5. Creates a Match with status=ACCEPTED immediately (no carrier confirmation needed).\n" +
                          "6. Creates a Chat room for the match.\n" +
                          "7. Creates a MatchReceiver if receiverNeeded=true (receiver block required in that case).\n" +
                          "8. Notifies the carrier via WebSocket.\n" +
                          "Returns the created Match."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposal accepted — Match created",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Proposal is not PENDING, or receiver details missing"),
            @ApiResponse(responseCode = "403", description = "Request belongs to another sender"),
            @ApiResponse(responseCode = "404", description = "Proposal not found")
    })
    @PostMapping("/offer-requests/proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponseDto<MatchResponseDto>> acceptProposal(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Proposal ID") @PathVariable UUID proposalId,
            @RequestBody AcceptProposalRequestDto dto) {

        MatchResponseDto match = offerProposalService.acceptProposal(principal.getUsername(), proposalId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(match));
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get proposals for a request",
            description = "Returns all proposals submitted on the sender's offer request. " +
                          "Only the request owner can view proposals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposals returned"),
            @ApiResponse(responseCode = "403", description = "Request belongs to another sender"),
            @ApiResponse(responseCode = "404", description = "Offer request not found")
    })
    @GetMapping("/offer-requests/{requestId}/proposals")
    public ResponseEntity<ApiResponseDto<List<OfferProposalResponseDto>>> getProposalsForRequest(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Offer request ID") @PathVariable UUID requestId) {

        return ResponseEntity.ok(ApiResponseDto.ok(
                offerProposalService.getProposalsForRequest(principal.getUsername(), requestId)));
    }

    @Operation(
            summary = "Get my proposals",
            description = "Returns all proposals submitted by the authenticated carrier, in all statuses."
    )
    @GetMapping("/proposals/me")
    public ResponseEntity<ApiResponseDto<List<OfferProposalResponseDto>>> getMyProposals(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(ApiResponseDto.ok(
                offerProposalService.getMyProposals(principal.getUsername())));
    }
}
