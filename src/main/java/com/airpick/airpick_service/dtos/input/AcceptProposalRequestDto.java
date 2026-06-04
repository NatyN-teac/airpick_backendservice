package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for a sender accepting a carrier's proposal.
 * <p>
 * When {@code receiverNeeded} is true, the {@code receiver} block must be provided
 * with the third-party receiver's details. When false, the sender is their own receiver
 * and the receiver block is ignored.
 */
public record AcceptProposalRequestDto(
        boolean receiverNeeded,
        ReceiverDto receiver
) {
    public record ReceiverDto(
            String firstName,
            String lastName,
            String phone,
            String photoIdUrl
    ) {}
}
