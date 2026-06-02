package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.ModeType;

/**
 * Input DTO for switching a user's active mode.
 */
public record UpdateModeRequestDto(
        ModeType mode
) {}
