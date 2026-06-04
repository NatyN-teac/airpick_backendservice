package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.OfferProposal;
import com.airpick.airpick_service.models.OfferProposalItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OfferProposalResponseDto(
        UUID id,
        UUID offerRequestId,
        UUID carrierId,
        FlightResponseDto flight,
        String status,
        String deliveryArea,
        String pickupArea,
        BigDecimal discount,
        List<String> meetupPlaces,
        List<String> paymentMethods,
        String note,
        List<ProposalItemDto> items,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ProposalItemDto(
            UUID id,
            UUID offerRequestItemId,
            String itemName,
            String measurementUnit,
            BigDecimal quantity,
            BigDecimal pricePerItem,
            BigDecimal lineTotal
    ) {
        public static ProposalItemDto from(OfferProposalItem pi) {
            BigDecimal qty = pi.getOfferRequestItem().getQuantity();
            BigDecimal lineTotal = qty.multiply(pi.getPricePerItem());
            return new ProposalItemDto(
                    pi.getId(),
                    pi.getOfferRequestItem().getId(),
                    pi.getOfferRequestItem().getItem().getName(),
                    pi.getOfferRequestItem().getItem().getMeasurementUnit().name(),
                    qty,
                    pi.getPricePerItem(),
                    lineTotal
            );
        }
    }

    public static OfferProposalResponseDto from(OfferProposal p) {
        List<ProposalItemDto> itemDtos = p.getItems().stream()
                .map(ProposalItemDto::from)
                .toList();

        BigDecimal totalPrice = itemDtos.stream()
                .map(ProposalItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OfferProposalResponseDto(
                p.getId(),
                p.getOfferRequest().getId(),
                p.getCarrier().getId(),
                FlightResponseDto.from(p.getFlight()),
                p.getStatus().name(),
                p.getDeliveryArea() != null ? p.getDeliveryArea() : p.getOfferRequest().getDestinationCountry(),
                p.getPickupArea() != null ? p.getPickupArea() : p.getOfferRequest().getSourceCountry(),
                p.getDiscount(),
                p.getMeetupPlaces(),
                p.getPaymentMethods().stream().map(Enum::name).toList(),
                p.getNote(),
                itemDtos,
                totalPrice,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
