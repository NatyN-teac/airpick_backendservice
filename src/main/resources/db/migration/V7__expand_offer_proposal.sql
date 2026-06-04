-- ============================================================
-- OfferRequest: add partial proposal flag
-- ============================================================
ALTER TABLE offer_requests
    ADD COLUMN partial_proposal_accepted BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================
-- OfferProposal: add carrier delivery terms
-- delivery_area and pickup_area are optional on the proposal;
-- they fall back to offerRequest destination/source if null.
-- ============================================================
ALTER TABLE offer_proposals
    ADD COLUMN delivery_area VARCHAR(200),
    ADD COLUMN pickup_area   VARCHAR(200),
    ADD COLUMN discount      DECIMAL(5,2);

CREATE TABLE offer_proposal_meetup_places (
    offer_proposal_id UUID         NOT NULL REFERENCES offer_proposals(id),
    place             VARCHAR(200) NOT NULL
);

CREATE INDEX idx_offer_proposal_meetup_places ON offer_proposal_meetup_places(offer_proposal_id);

CREATE TABLE offer_proposal_payment_methods (
    offer_proposal_id UUID        NOT NULL REFERENCES offer_proposals(id),
    payment_method    VARCHAR(30) NOT NULL
);

CREATE INDEX idx_offer_proposal_payment_methods ON offer_proposal_payment_methods(offer_proposal_id);

-- ============================================================
-- OfferProposalItem: swap item_id + quantity for offer_request_item_id
-- The carrier prices the sender's items; quantity comes from the request item.
-- Table has no data yet (service not implemented), safe to alter directly.
-- ============================================================
ALTER TABLE offer_proposal_items
    DROP COLUMN item_id,
    DROP COLUMN quantity,
    ADD COLUMN offer_request_item_id UUID NOT NULL REFERENCES offer_request_items(id);

CREATE INDEX idx_offer_proposal_items_request_item ON offer_proposal_items(offer_request_item_id);
