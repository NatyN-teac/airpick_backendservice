package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Item;

import java.util.UUID;

public record ItemResponseDto(
        UUID id,
        String name,
        String category,
        String measurementType,
        String measurementUnit,
        boolean isApproved,
        boolean isManuallyCreated
) {
    public static ItemResponseDto from(Item item) {
        return new ItemResponseDto(
                item.getId(),
                item.getName(),
                item.getCategory().name(),
                item.getMeasurementType().name(),
                item.getMeasurementUnit().name(),
                item.isApproved(),
                item.getCreatedBy() != null
        );
    }
}
