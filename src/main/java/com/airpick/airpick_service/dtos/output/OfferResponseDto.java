package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Offer;
import com.airpick.airpick_service.models.OfferItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OfferResponseDto(
        UUID id,
        UUID carrierId,
        String offerSource,
        String status,
        FlightResponseDto flight,
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
                offer.getOfferSource().name(),
                offer.getStatus().name(),
                FlightResponseDto.from(offer.getFlight()),
                offer.getDeliveryArea(),
                offer.getPickupArea(),
                offer.getUrgencyLevel().name(),
                offer.getDiscount(),
                offer.getSpecialNote(),
                offer.getMeetupPlaces(),
                offer.getPaymentMethods().stream().map(Enum::name).toList(),
                offer.getOfferItems().stream().map(OfferItemDto::from).toList(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
