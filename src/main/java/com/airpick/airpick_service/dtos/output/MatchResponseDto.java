package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Match;
import com.airpick.airpick_service.models.MatchReceiver;
import com.airpick.airpick_service.models.MatchedItem;
import com.airpick.airpick_service.models.MatchedItemStatusHistory;
import com.airpick.airpick_service.models.MatchStatusHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MatchResponseDto(
        UUID id,
        UUID carrierId,
        UUID shipperId,
        UUID offerId,
        String status,
        BigDecimal totalPrice,
        boolean receiverNeeded,
        ReceiverDto receiver,
        UUID chatId,
        boolean hasPickupPhoto,
        LocalDateTime pickupPhotoUploadedAt,
        List<MatchedItemDto> matchedItems,
        List<MatchStatusHistoryDto> statusHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record ReceiverDto(
            String firstName,
            String lastName,
            String phone,
            String photoIdUrl,
            boolean isActive
    ) {
        public static ReceiverDto from(MatchReceiver r) {
            return new ReceiverDto(r.getFirstName(), r.getLastName(), r.getPhone(), r.getPhotoIdUrl(), r.isActive());
        }
    }

    public record MatchedItemDto(
            UUID id,
            UUID offerItemId,
            ItemSummaryDto item,
            BigDecimal quantity,
            BigDecimal pricePerItem,
            String status,
            List<MatchedItemStatusHistoryDto> statusHistory
    ) {
        public static MatchedItemDto from(MatchedItem mi) {
            return new MatchedItemDto(
                    mi.getId(),
                    mi.getOfferItem().getId(),
                    ItemSummaryDto.from(mi),
                    mi.getQuantity(),
                    mi.getOfferItem().getPricePerItem(),
                    mi.getStatus().name(),
                    mi.getStatusHistory().stream().map(MatchedItemStatusHistoryDto::from).toList()
            );
        }
    }

    public record ItemSummaryDto(
            UUID id,
            String name,
            String category,
            String measurementUnit
    ) {
        public static ItemSummaryDto from(MatchedItem mi) {
            return new ItemSummaryDto(
                    mi.getOfferItem().getItem().getId(),
                    mi.getOfferItem().getItem().getName(),
                    mi.getOfferItem().getItem().getCategory().name(),
                    mi.getOfferItem().getItem().getMeasurementUnit().name()
            );
        }
    }

    public record MatchStatusHistoryDto(
            UUID changedById,
            String previousStatus,
            String newStatus,
            String note,
            LocalDateTime changedAt
    ) {
        public static MatchStatusHistoryDto from(MatchStatusHistory h) {
            return new MatchStatusHistoryDto(
                    h.getChangedBy().getId(),
                    h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null,
                    h.getNewStatus().name(),
                    h.getNote(),
                    h.getChangedAt()
            );
        }
    }

    public record MatchedItemStatusHistoryDto(
            UUID changedById,
            String previousStatus,
            String newStatus,
            String note,
            LocalDateTime changedAt
    ) {
        public static MatchedItemStatusHistoryDto from(MatchedItemStatusHistory h) {
            return new MatchedItemStatusHistoryDto(
                    h.getChangedBy().getId(),
                    h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null,
                    h.getNewStatus().name(),
                    h.getNote(),
                    h.getChangedAt()
            );
        }
    }

    public static MatchResponseDto from(Match match) {
        ReceiverDto receiverDto = match.isReceiverNeeded() && match.getReceiver() != null
                ? ReceiverDto.from(match.getReceiver())
                : null;

        return new MatchResponseDto(
                match.getId(),
                match.getCarrier().getId(),
                match.getShipper().getId(),
                match.getOffer().getId(),
                match.getStatus().name(),
                match.getTotalPrice(),
                match.isReceiverNeeded(),
                receiverDto,
                match.getChat() != null ? match.getChat().getId() : null,
                match.getPickupPhotoObjectPath() != null,
                match.getPickupPhotoUploadedAt(),
                match.getMatchedItems().stream().map(MatchedItemDto::from).toList(),
                match.getStatusHistory().stream().map(MatchStatusHistoryDto::from).toList(),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }
}
