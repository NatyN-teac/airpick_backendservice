package com.airpick.airpick_service.dtos.output;

import java.util.List;

public record ApiResponseDto<T>(
        boolean success,
        T content,
        PaginationDto pagination,
        ErrorDto errors,
        MetaDto meta
) {
    public static <T> ApiResponseDto<T> ok(T content) {
        return new ApiResponseDto<>(true, content, null, null, MetaDto.now());
    }

    public static <T> ApiResponseDto<T> ok() {
        return new ApiResponseDto<>(true, null, null, null, MetaDto.now());
    }

    public static <T> ApiResponseDto<List<T>> paged(List<T> items, PaginationDto pagination) {
        return new ApiResponseDto<>(true, items, pagination, null, MetaDto.now());
    }

    public static <T> ApiResponseDto<T> error(ErrorDto error) {
        return new ApiResponseDto<>(false, null, null, error, MetaDto.now());
    }
}
