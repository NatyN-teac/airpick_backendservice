package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.ModeType;

public record RegisterRequestDto(
        String firebaseToken,
        ModeType mode
) {}
