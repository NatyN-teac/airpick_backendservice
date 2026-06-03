package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.PaymentMethod;
import com.airpick.airpick_service.models.UrgencyLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input DTO for creating a new offer (carrier-initiated, DIRECT source).
 */
public record CreateOfferRequestDto(
        UUID flightId,
        String deliveryArea,
        String pickupArea,
        UrgencyLevel urgencyLevel,
        BigDecimal discount,
        String specialNote,
        List<String> meetupPlaces,
        List<PaymentMethod> paymentMethods,
        List<OfferItemDto> items
) {
    public record OfferItemDto(
            UUID itemId,
            BigDecimal quantity,
            BigDecimal pricePerItem
    ) {}
}
