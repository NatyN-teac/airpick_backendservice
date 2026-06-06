package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.UrgencyLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input DTO for updating an existing offer.
 * All fields are optional — only non-null values are applied.
 * Offer items cannot be updated here; items are managed separately.
 */
public record UpdateOfferRequestDto(
        String currency,
        String deliveryArea,
        String pickupArea,
        UrgencyLevel urgencyLevel,
        BigDecimal discount,
        String specialNote,
        List<String> meetupPlaces,
        List<String> paymentMethods
) {}
