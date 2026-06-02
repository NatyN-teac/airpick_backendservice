package com.airpick.airpick_service.models;

public enum OfferSource {
    /** Carrier created the offer directly on the marketplace. */
    DIRECT,
    /** Offer was created as a result of a carrier accepting a sender's OfferRequest proposal. */
    FROM_PROPOSAL
}
