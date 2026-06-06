package com.airpick.airpick_service.models;

public enum OfferStatus {
    PENDING_ITEM_APPROVAL, // offer contains manually-created items awaiting admin approval
    OPEN,
    FULLY_MATCHED,
    CLOSED,
    CANCELLED
}
