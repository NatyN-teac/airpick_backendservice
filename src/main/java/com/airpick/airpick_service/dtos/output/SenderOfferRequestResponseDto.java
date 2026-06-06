package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.OfferRequest;
import com.airpick.airpick_service.models.OfferRequestItem;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SenderOfferRequestResponseDto(
        UUID id,
        ShipperDto shipper,
        String sourceCountry,
        String sourceCity,
        String destinationCountry,
        LocalDate preferredDate,
        String urgencyLevel,
        String specialNote,
        String status,
        boolean partialProposalAccepted,
        boolean hasManualItem,
        List<RequestItemDto> items,
        int proposalCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ShipperDto(
            UUID id,
            String firstName,
            String lastName,
            String profilePictureUrl
    ) {
        public static ShipperDto from(User user) {
            UserProfile profile = user.getUserProfile();
            return new ShipperDto(
                    user.getId(),
                    profile != null ? profile.getFirstName() : null,
                    profile != null ? profile.getLastName() : null,
                    profile != null ? profile.getProfilePictureUrl() : null
            );
        }
    }

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
                ShipperDto.from(r.getShipper()),
                r.getSourceCountry(),
                r.getSourceCity(),
                r.getDestinationCountry(),
                r.getPreferredDate(),
                r.getUrgencyLevel().name(),
                r.getSpecialNote(),
                r.getStatus().name(),
                r.isPartialProposalAccepted(),
                r.isHasManualItem(),
                r.getItems().stream().map(RequestItemDto::from).toList(),
                r.getProposals().size(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
