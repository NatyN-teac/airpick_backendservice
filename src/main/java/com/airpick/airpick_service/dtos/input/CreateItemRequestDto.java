package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.ItemCategory;
import com.airpick.airpick_service.models.MeasurementType;
import com.airpick.airpick_service.models.MeasurementUnit;

/**
 * Input DTO for carrier-submitted item requests.
 * Submitted items start as unapproved and require admin approval before
 * they appear in the catalogue.
 */
public record CreateItemRequestDto(
        String name,
        ItemCategory category,
        MeasurementType measurementType,
        MeasurementUnit measurementUnit
) {}
