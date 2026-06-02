package com.airpick.airpick_service.dtos.output;

public record PaginationDto(
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
