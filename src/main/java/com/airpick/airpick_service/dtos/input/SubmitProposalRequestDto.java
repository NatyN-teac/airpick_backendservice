package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for a carrier submitting a price proposal on a sender's offer request.
 * <p>
 * {@code items} must cover all request items unless the request has {@code partialProposalAccepted = true}.
 * {@code deliveryArea} and {@code pickupArea} are optional — they fall back to
 * the request's destinationCountry and sourceCountry respectively when null.
 * {@code meetupPlaces} and {@code paymentMethods} are required.
 */
public record SubmitProposalRequestDto(
        UUID flightId,
        String deliveryArea,
        String pickupArea,
        BigDecimal discount,
        List<String> meetupPlaces,
        List<PaymentMethod> paymentMethods,
        String note,
        List<ProposalItemDto> items
) {
    public record ProposalItemDto(
            UUID offerRequestItemId,
            BigDecimal pricePerItem
    ) {}
}
