package com.airpick.airpick_service.integrations.veriff;

import java.time.LocalDate;
import java.util.UUID;

public record VeriffSessionRequest(
        UUID endUserId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
