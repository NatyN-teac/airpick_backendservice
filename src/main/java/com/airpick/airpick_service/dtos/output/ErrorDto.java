package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.commons.exceptions.ErrorCode;

import java.util.Map;

public record ErrorDto(
        String message,
        String errorCode,
        Map<String, Object> details
) {
    public static ErrorDto of(ErrorCode code, String message) {
        return new ErrorDto(message, code.getCode(), null);
    }

    public static ErrorDto of(ErrorCode code, String message, Map<String, Object> details) {
        return new ErrorDto(message, code.getCode(), details);
    }
}
