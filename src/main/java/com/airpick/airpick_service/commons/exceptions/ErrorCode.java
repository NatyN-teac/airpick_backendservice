package com.airpick.airpick_service.commons.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_FIREBASE_TOKEN("AUTH_001"),
    EMAIL_ALREADY_EXISTS("AUTH_002"),
    USER_BLOCKED("AUTH_003"),
    USER_INACTIVE("AUTH_004"),

    // Data
    ROLE_NOT_FOUND("DATA_001"),
    MODE_NOT_FOUND("DATA_002"),
    USER_NOT_FOUND("DATA_003"),

    // System
    INTERNAL_ERROR("SYS_001");

    private final String code;
}
