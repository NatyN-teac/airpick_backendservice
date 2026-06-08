package com.airpick.airpick_service.commons.exceptions;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.ErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Central exception handler — translates service-layer exceptions into
 * clean, consistent API responses. Internal details (stack traces, class names,
 * DB messages) never reach the response body.
 * <p>
 * Logging levels:
 * <ul>
 *   <li>WARN  — client errors (4xx): bad input, wrong state, not found</li>
 *   <li>ERROR — server errors (5xx): unexpected exceptions</li>
 * </ul>
 * Stack traces are always attached to the log so they appear in the server log
 * without leaking to the caller.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Business rule violation passed as validation input error — 400.
     * e.g. "Offer not found", "Item not available", "Receiver details required".
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorCode code = resolveErrorCode(ex.getMessage());
        HttpStatus status = resolveHttpStatus(code);

        // Firebase token errors are frequent, expected client events — log without stack trace
        if (code == ErrorCode.INVALID_FIREBASE_TOKEN) {
            log.debug("[{}] Firebase token rejected: {}", originOf(ex), ex.getMessage());
        } else {
            log.warn("[{}] Bad request: {}", originOf(ex), ex.getMessage(), ex);
        }

        return ResponseEntity.status(status)
                .body(ApiResponseDto.error(ErrorDto.of(code, ex.getMessage())));
    }

    /**
     * Business state violation — entity is in the wrong state for the requested action — 400.
     * e.g. "Cannot accept a REJECTED proposal", "Offer is not OPEN".
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("[{}] Invalid state transition: {}", originOf(ex), ex.getMessage(), ex);

        ErrorCode code = resolveErrorCode(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ErrorDto.of(code, ex.getMessage())));
    }

    /**
     * Resource not found — 404.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNotFound(NoSuchElementException ex) {
        log.warn("[{}] Resource not found: {}", originOf(ex), ex.getMessage(), ex);

        ErrorCode code = resolveErrorCode(ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(ErrorDto.of(code, ex.getMessage())));
    }

    /**
     * Client disconnected before the response was fully sent — nothing to write back.
     * Log at DEBUG only; this is normal network noise, not an application error.
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex) {
        log.debug("Client disconnected before response was sent: {}", ex.getMessage());
        // no response — the connection is already gone
    }

    /**
     * Catch-all for anything not explicitly handled.
     * The raw exception message is NOT forwarded to the caller — only a generic
     * message is returned so internal details stay server-side.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneric(Exception ex) {
        log.error("[{}] Unhandled {}: {}", originOf(ex), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                        ErrorDto.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred. Please try again later.")));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts "ClassName.methodName:line" from the top of the exception's stack trace
     * so the log line immediately shows where the exception was thrown without having
     * to scroll through the full stack trace.
     * Example output: "OfferService.createOffer:58"
     */
    private String originOf(Throwable ex) {
        StackTraceElement[] frames = ex.getStackTrace();
        if (frames == null || frames.length == 0) return "unknown";
        StackTraceElement top = frames[0];
        String simpleClass = top.getClassName()
                .substring(top.getClassName().lastIndexOf('.') + 1);
        return simpleClass + "." + top.getMethodName() + ":" + top.getLineNumber();
    }

    private ErrorCode resolveErrorCode(String message) {
        if (message == null) return ErrorCode.INTERNAL_ERROR;

        // Auth
        if (message.contains("Firebase token"))        return ErrorCode.INVALID_FIREBASE_TOKEN;
        if (message.contains("already registered"))    return ErrorCode.EMAIL_ALREADY_EXISTS;
        if (message.contains("blocked"))               return ErrorCode.USER_BLOCKED;
        if (message.contains("inactive"))              return ErrorCode.USER_INACTIVE;

        // Not found
        if (message.contains("User not found"))        return ErrorCode.USER_NOT_FOUND;
        if (message.contains("Offer not found"))       return ErrorCode.OFFER_NOT_FOUND;
        if (message.contains("Match not found"))       return ErrorCode.MATCH_NOT_FOUND;
        if (message.contains("Offer request not found")) return ErrorCode.OFFER_REQUEST_NOT_FOUND;
        if (message.contains("Proposal not found"))    return ErrorCode.PROPOSAL_NOT_FOUND;
        if (message.contains("Flight not found"))      return ErrorCode.FLIGHT_NOT_FOUND;
        if (message.contains("Item not found"))        return ErrorCode.ITEM_NOT_FOUND;
        if (message.contains("Airport not found"))     return ErrorCode.AIRPORT_NOT_FOUND;
        if (message.contains("Notification not found")) return ErrorCode.NOTIFICATION_NOT_FOUND;
        if (message.contains("Offer item not found"))  return ErrorCode.ITEM_NOT_FOUND;
        if (message.contains("CUSTOMER role"))         return ErrorCode.ROLE_NOT_FOUND;
        if (message.contains("Mode"))                  return ErrorCode.MODE_NOT_FOUND;

        // Business rules
        if (message.contains("status")        ||
            message.contains("Cannot cancel") ||
            message.contains("Cannot update") ||
            message.contains("already cancelled") ||
            message.contains("already accepted"))     return ErrorCode.INVALID_STATUS_TRANSITION;
        if (message.contains("exceeds available"))    return ErrorCode.INSUFFICIENT_QUANTITY;
        if (message.contains("partial proposal") ||
            message.contains("must price all"))       return ErrorCode.PARTIAL_PROPOSAL_NOT_ALLOWED;
        if (message.contains("active proposal"))      return ErrorCode.DUPLICATE_PROPOSAL;
        if (message.contains("own offer")      ||
            message.contains("own request")    ||
            message.contains("own offer request"))    return ErrorCode.SELF_ACTION_NOT_ALLOWED;
        if (message.contains("not available"))        return ErrorCode.ITEM_NOT_AVAILABLE;
        if (message.contains("Receiver details"))     return ErrorCode.RECEIVER_DETAILS_REQUIRED;
        if (message.contains("does not belong"))      return ErrorCode.ACCESS_DENIED;
        if (message.contains("Invalid Veriff webhook")) return ErrorCode.INVALID_WEBHOOK_SIGNATURE;
        if (message.contains("already verified"))   return ErrorCode.USER_ALREADY_VERIFIED;
        if (message.contains("Veriff integration is not configured")) return ErrorCode.INTERNAL_ERROR;
        if (message.contains("Cloud storage is not configured")) return ErrorCode.STORAGE_NOT_CONFIGURED;
        if (message.contains("Unsupported image type") ||
            message.contains("Photo file"))              return ErrorCode.INVALID_FILE;
        if (message.contains("Pickup photo is required")) return ErrorCode.PICKUP_PHOTO_REQUIRED;

        return ErrorCode.INTERNAL_ERROR;
    }

    private HttpStatus resolveHttpStatus(ErrorCode code) {
        return switch (code) {
            case INVALID_FIREBASE_TOKEN         -> HttpStatus.UNAUTHORIZED;
            case EMAIL_ALREADY_EXISTS           -> HttpStatus.CONFLICT;
            case DUPLICATE_PROPOSAL             -> HttpStatus.CONFLICT;
            case USER_BLOCKED,
                 USER_INACTIVE,
                 ACCESS_DENIED,
                 INVALID_WEBHOOK_SIGNATURE      -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND,
                 OFFER_NOT_FOUND,
                 MATCH_NOT_FOUND,
                 OFFER_REQUEST_NOT_FOUND,
                 PROPOSAL_NOT_FOUND,
                 FLIGHT_NOT_FOUND,
                 ITEM_NOT_FOUND,
                 AIRPORT_NOT_FOUND,
                 NOTIFICATION_NOT_FOUND,
                 CHAT_NOT_FOUND                 -> HttpStatus.NOT_FOUND;
            case INVALID_STATUS_TRANSITION,
                 INSUFFICIENT_QUANTITY,
                 PARTIAL_PROPOSAL_NOT_ALLOWED,
                 SELF_ACTION_NOT_ALLOWED,
                 ITEM_NOT_AVAILABLE,
                 RECEIVER_DETAILS_REQUIRED,
                 MODE_NOT_FOUND,
                 ROLE_NOT_FOUND,
                 USER_ALREADY_VERIFIED,
                 PICKUP_PHOTO_REQUIRED,
                 INVALID_FILE,
                 STORAGE_NOT_CONFIGURED          -> HttpStatus.BAD_REQUEST;
            default                             -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
