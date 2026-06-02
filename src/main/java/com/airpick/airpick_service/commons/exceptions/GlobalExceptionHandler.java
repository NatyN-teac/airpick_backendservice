package com.airpick.airpick_service.commons.exceptions;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.ErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        ErrorCode code = resolveErrorCode(ex.getMessage());
        HttpStatus status = resolveHttpStatus(code);

        return ResponseEntity.status(status)
                .body(ApiResponseDto.error(ErrorDto.of(code, ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalState(IllegalStateException ex) {
        log.error("Data integrity issue: {}", ex.getMessage());

        ErrorCode code = resolveErrorCode(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(ErrorDto.of(code, ex.getMessage())));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(ErrorDto.of(ErrorCode.USER_NOT_FOUND, ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                        ErrorDto.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred",
                                Map.of("cause", ex.getClass().getSimpleName()))
                ));
    }

    private ErrorCode resolveErrorCode(String message) {
        if (message == null) return ErrorCode.INTERNAL_ERROR;
        if (message.contains("Firebase token"))     return ErrorCode.INVALID_FIREBASE_TOKEN;
        if (message.contains("already registered")) return ErrorCode.EMAIL_ALREADY_EXISTS;
        if (message.contains("blocked"))            return ErrorCode.USER_BLOCKED;
        if (message.contains("inactive"))           return ErrorCode.USER_INACTIVE;
        if (message.contains("CUSTOMER role"))      return ErrorCode.ROLE_NOT_FOUND;
        if (message.contains("Mode"))               return ErrorCode.MODE_NOT_FOUND;
        if (message.contains("User not found"))     return ErrorCode.USER_NOT_FOUND;
        return ErrorCode.INTERNAL_ERROR;
    }

    private HttpStatus resolveHttpStatus(ErrorCode code) {
        return switch (code) {
            case INVALID_FIREBASE_TOKEN -> HttpStatus.UNAUTHORIZED;
            case EMAIL_ALREADY_EXISTS   -> HttpStatus.CONFLICT;
            case USER_BLOCKED           -> HttpStatus.FORBIDDEN;
            case USER_INACTIVE          -> HttpStatus.FORBIDDEN;
            case MODE_NOT_FOUND         -> HttpStatus.BAD_REQUEST;
            case USER_NOT_FOUND         -> HttpStatus.NOT_FOUND;
            default                     -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
