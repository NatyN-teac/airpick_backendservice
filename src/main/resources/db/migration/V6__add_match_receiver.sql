-- ============================================================
-- Matches: add receiver_needed flag
-- ============================================================
ALTER TABLE matches ADD COLUMN receiver_needed BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================
-- Match Receivers
-- One-to-one with matches. Created when receiverNeeded = true.
-- Linked to the shipper (primary sender) who registered the receiver.
-- is_active transitions false → true when the carrier accepts the match.
-- ============================================================
CREATE TABLE match_receivers (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id     UUID         NOT NULL UNIQUE REFERENCES matches(id),
    shipper_id   UUID         NOT NULL REFERENCES users(id),
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    phone        VARCHAR(30)  NOT NULL,
    photo_id_url TEXT         NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_receivers_match   ON match_receivers(match_id);
CREATE INDEX idx_match_receivers_shipper ON match_receivers(shipper_id);
