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

    // Data — lookup failures
    ROLE_NOT_FOUND("DATA_001"),
    MODE_NOT_FOUND("DATA_002"),
    USER_NOT_FOUND("DATA_003"),
    OFFER_NOT_FOUND("DATA_004"),
    MATCH_NOT_FOUND("DATA_005"),
    OFFER_REQUEST_NOT_FOUND("DATA_006"),
    PROPOSAL_NOT_FOUND("DATA_007"),
    FLIGHT_NOT_FOUND("DATA_008"),
    ITEM_NOT_FOUND("DATA_009"),
    AIRPORT_NOT_FOUND("DATA_010"),
    NOTIFICATION_NOT_FOUND("DATA_011"),
    CHAT_NOT_FOUND("DATA_012"),

    // Business rules — invalid state transitions or constraint violations
    INVALID_STATUS_TRANSITION("BIZ_001"),
    INSUFFICIENT_QUANTITY("BIZ_002"),
    PARTIAL_PROPOSAL_NOT_ALLOWED("BIZ_003"),
    DUPLICATE_PROPOSAL("BIZ_004"),
    SELF_ACTION_NOT_ALLOWED("BIZ_005"),
    ITEM_NOT_AVAILABLE("BIZ_006"),
    RECEIVER_DETAILS_REQUIRED("BIZ_007"),

    // Access control
    ACCESS_DENIED("ACL_001"),

    // System
    INTERNAL_ERROR("SYS_001");

    private final String code;
}
