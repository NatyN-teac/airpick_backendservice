package com.airpick.airpick_service.dtos.output;

import java.util.List;

/**
 * Active engagement summary for the authenticated user, shaped by their current mode.
 * <p>
 * CARRIER: {@code proposalsSent} + {@code matchedOffers}
 * SHIPPER: {@code proposalsReceived} + {@code matchedOffers}
 * <p>
 * Each list is capped at 5 items, most recently updated first.
 */
public record EngagementResponseDto(
        String mode,
        List<OfferProposalResponseDto> proposalsSent,
        List<OfferProposalResponseDto> proposalsReceived,
        List<MatchResponseDto> matchedOffers
) {}
