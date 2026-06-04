package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.OfferRequest;
import com.airpick.airpick_service.models.OfferRequestItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SenderOfferRequestResponseDto(
        UUID id,
        UUID shipperId,
        String sourceCountry,
        String destinationCountry,
        AirportResponseDto srcAirport,
        AirportResponseDto destAirport,
        LocalDate preferredDate,
        String urgencyLevel,
        String specialNote,
        String status,
        boolean partialProposalAccepted,
        List<RequestItemDto> items,
        int proposalCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record RequestItemDto(
            UUID id,
            UUID itemId,
            String itemName,
            String category,
            String measurementType,
            String measurementUnit,
            BigDecimal quantity
    ) {
        public static RequestItemDto from(OfferRequestItem ri) {
            return new RequestItemDto(
                    ri.getId(),
                    ri.getItem().getId(),
                    ri.getItem().getName(),
                    ri.getItem().getCategory().name(),
                    ri.getItem().getMeasurementType().name(),
                    ri.getItem().getMeasurementUnit().name(),
                    ri.getQuantity()
            );
        }
    }

    public static SenderOfferRequestResponseDto from(OfferRequest r) {
        return new SenderOfferRequestResponseDto(
                r.getId(),
                r.getShipper().getId(),
                r.getSourceCountry(),
                r.getDestinationCountry(),
                r.getSrcAirport() != null ? AirportResponseDto.from(r.getSrcAirport()) : null,
                r.getDestAirport() != null ? AirportResponseDto.from(r.getDestAirport()) : null,
                r.getPreferredDate(),
                r.getUrgencyLevel().name(),
                r.getSpecialNote(),
                r.getStatus().name(),
                r.isPartialProposalAccepted(),
                r.getItems().stream().map(RequestItemDto::from).toList(),
                r.getProposals().size(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
