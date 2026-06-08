package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Offer;
import com.airpick.airpick_service.models.OfferItem;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OfferResponseDto(
        UUID id,
        UUID carrierId,
        CarrierDto carrier,
        String offerSource,
        String status,
        boolean hasManualItem,
        FlightResponseDto flight,
        String currency,
        String deliveryArea,
        String pickupArea,
        String urgencyLevel,
        BigDecimal discount,
        String specialNote,
        List<String> meetupPlaces,
        List<String> paymentMethods,
        List<OfferItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CarrierDto(
            String firstName,
            String lastName,
            String profilePictureUrl,
            boolean isVerified,
            BigDecimal rating
    ) {
        public static CarrierDto from(User user) {
            UserProfile profile = user.getUserProfile();
            if (profile == null) {
                return new CarrierDto(null, null, null, false, null);
            }
            return new CarrierDto(
                    profile.getFirstName(),
                    profile.getLastName(),
                    profile.getProfilePictureUrl(),
                    profile.isVerified(),
                    profile.getRating()
            );
        }
    }

    public record OfferItemDto(
            UUID id,
            ItemDto item,
            BigDecimal quantity,
            BigDecimal remainingQuantity,
            BigDecimal pricePerItem
    ) {
        public static OfferItemDto from(OfferItem oi) {
            return new OfferItemDto(
                    oi.getId(),
                    ItemDto.from(oi),
                    oi.getQuantity(),
                    oi.getRemainingQuantity(),
                    oi.getPricePerItem()
            );
        }
    }

    public record ItemDto(
            UUID id,
            String name,
            String category,
            String measurementType,
            String measurementUnit
    ) {
        public static ItemDto from(OfferItem oi) {
            return new ItemDto(
                    oi.getItem().getId(),
                    oi.getItem().getName(),
                    oi.getItem().getCategory().name(),
                    oi.getItem().getMeasurementType().name(),
                    oi.getItem().getMeasurementUnit().name()
            );
        }
    }

    public static OfferResponseDto from(Offer offer) {
        return new OfferResponseDto(
                offer.getId(),
                offer.getCarrier().getId(),
                CarrierDto.from(offer.getCarrier()),
                offer.getOfferSource().name(),
                offer.getStatus().name(),
                offer.isHasManualItem(),
                FlightResponseDto.from(offer.getFlight()),
                offer.getCurrency(),
                offer.getDeliveryArea(),
                offer.getPickupArea(),
                offer.getUrgencyLevel().name(),
                offer.getDiscount(),
                offer.getSpecialNote(),
                offer.getMeetupPlaces(),
                offer.getPaymentMethods(),
                offer.getOfferItems().stream().map(OfferItemDto::from).toList(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
