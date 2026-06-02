package com.airpick.airpick_service.dtos.output;

import java.time.LocalDateTime;

public record MetaDto(
        LocalDateTime timestamp
) {
    public static MetaDto now() {
        return new MetaDto(LocalDateTime.now());
    }
}
