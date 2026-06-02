-- ============================================================
-- Users: add cancellation tracking
-- ============================================================
ALTER TABLE users ADD COLUMN cancellation_count INT NOT NULL DEFAULT 0;

-- ============================================================
-- Items  (admin-managed; carriers may submit for approval)
-- ============================================================
CREATE TABLE items (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    category         VARCHAR(50)  NOT NULL,
    measurement_type VARCHAR(50)  NOT NULL,
    measurement_unit VARCHAR(50)  NOT NULL,
    max_allowed      DECIMAL(10,2),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_approved      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by       UUID         REFERENCES users(id),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_items_active_approved ON items(is_active, is_approved);

-- ============================================================
-- Offer Requests  (sender-initiated, looking for a carrier)
-- ============================================================
CREATE TABLE offer_requests (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    shipper_id           UUID         NOT NULL REFERENCES users(id),
    source_country       VARCHAR(100) NOT NULL,
    destination_country  VARCHAR(100) NOT NULL,
    src_airport_id       UUID         REFERENCES airports(id),
    dest_airport_id      UUID         REFERENCES airports(id),
    preferred_date       DATE,
    urgency_level        VARCHAR(20)  NOT NULL,
    special_note         TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offer_requests_shipper  ON offer_requests(shipper_id);
CREATE INDEX idx_offer_requests_status   ON offer_requests(status);

-- ============================================================
-- Offer Request Items
-- ============================================================
CREATE TABLE offer_request_items (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_request_id UUID         NOT NULL REFERENCES offer_requests(id),
    item_id          UUID         NOT NULL REFERENCES items(id),
    quantity         DECIMAL(10,2) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offer_request_items_request ON offer_request_items(offer_request_id);

-- ============================================================
-- Offer Proposals  (carrier responds to an offer request)
-- ============================================================
CREATE TABLE offer_proposals (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_request_id UUID        NOT NULL REFERENCES offer_requests(id),
    carrier_id       UUID        NOT NULL REFERENCES users(id),
    flight_id        UUID        NOT NULL REFERENCES flights(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note             TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offer_proposals_request ON offer_proposals(offer_request_id);
CREATE INDEX idx_offer_proposals_carrier ON offer_proposals(carrier_id);

-- ============================================================
-- Offer Proposal Items
-- ============================================================
CREATE TABLE offer_proposal_items (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id    UUID          NOT NULL REFERENCES offer_proposals(id),
    item_id        UUID          NOT NULL REFERENCES items(id),
    quantity       DECIMAL(10,2) NOT NULL,
    price_per_item DECIMAL(10,2) NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offer_proposal_items_proposal ON offer_proposal_items(proposal_id);

-- ============================================================
-- Offers  (carrier-posted; DIRECT or FROM_PROPOSAL)
-- ============================================================
CREATE TABLE offers (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id       UUID         NOT NULL REFERENCES users(id),
    flight_id        UUID         NOT NULL REFERENCES flights(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    offer_source     VARCHAR(20)  NOT NULL DEFAULT 'DIRECT',
    offer_request_id UUID         REFERENCES offer_requests(id),
    delivery_area    VARCHAR(200) NOT NULL,
    pickup_area      VARCHAR(200) NOT NULL,
    urgency_level    VARCHAR(20)  NOT NULL,
    discount         DECIMAL(5,2),
    special_note     TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offers_carrier ON offers(carrier_id);
CREATE INDEX idx_offers_status  ON offers(status);

-- ============================================================
-- Offer Meetup Places  (@ElementCollection)
-- ============================================================
CREATE TABLE offer_meetup_places (
    offer_id UUID        NOT NULL REFERENCES offers(id),
    place    VARCHAR(300) NOT NULL
);

CREATE INDEX idx_offer_meetup_places_offer ON offer_meetup_places(offer_id);

-- ============================================================
-- Offer Payment Methods  (@ElementCollection)
-- ============================================================
CREATE TABLE offer_payment_methods (
    offer_id       UUID        NOT NULL REFERENCES offers(id),
    payment_method VARCHAR(30) NOT NULL
);

CREATE INDEX idx_offer_payment_methods_offer ON offer_payment_methods(offer_id);

-- ============================================================
-- Offer Items
-- ============================================================
CREATE TABLE offer_items (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id           UUID          NOT NULL REFERENCES offers(id),
    item_id            UUID          NOT NULL REFERENCES items(id),
    quantity           DECIMAL(10,2) NOT NULL,
    remaining_quantity DECIMAL(10,2) NOT NULL,
    price_per_item     DECIMAL(10,2) NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offer_items_offer ON offer_items(offer_id);

-- ============================================================
-- Matches  (carrier ↔ shipper agreement on an offer)
-- ============================================================
CREATE TABLE matches (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id  UUID          NOT NULL REFERENCES users(id),
    shipper_id  UUID          NOT NULL REFERENCES users(id),
    offer_id    UUID          NOT NULL REFERENCES offers(id),
    status      VARCHAR(20)   NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matches_carrier ON matches(carrier_id);
CREATE INDEX idx_matches_shipper ON matches(shipper_id);
CREATE INDEX idx_matches_offer   ON matches(offer_id);

-- ============================================================
-- Match Status History  (append-only audit log)
-- ============================================================
CREATE TABLE match_status_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id        UUID        NOT NULL REFERENCES matches(id),
    previous_status VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    changed_by      UUID        NOT NULL REFERENCES users(id),
    note            TEXT,
    changed_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_status_history_match ON match_status_history(match_id);

-- ============================================================
-- Matched Items
-- ============================================================
CREATE TABLE matched_items (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id      UUID          NOT NULL REFERENCES matches(id),
    offer_item_id UUID          NOT NULL REFERENCES offer_items(id),
    quantity      DECIMAL(10,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matched_items_match ON matched_items(match_id);

-- ============================================================
-- Matched Item Status History  (append-only audit log)
-- ============================================================
CREATE TABLE matched_item_status_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    matched_item_id UUID        NOT NULL REFERENCES matched_items(id),
    previous_status VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    changed_by      UUID        NOT NULL REFERENCES users(id),
    note            TEXT,
    changed_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matched_item_status_history_item ON matched_item_status_history(matched_item_id);

-- ============================================================
-- Chats  (one per match, created automatically)
-- ============================================================
CREATE TABLE chats (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id   UUID      NOT NULL UNIQUE REFERENCES matches(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Chat Messages
-- ============================================================
CREATE TABLE chat_messages (
    id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id   UUID      NOT NULL REFERENCES chats(id),
    sender_id UUID      NOT NULL REFERENCES users(id),
    content   TEXT      NOT NULL,
    is_read   BOOLEAN   NOT NULL DEFAULT FALSE,
    sent_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_chat ON chat_messages(chat_id);
