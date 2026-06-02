-- ============================================================
-- Airports  (admin-managed, referenced by flight legs)
-- ============================================================
CREATE TABLE airports (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(200) NOT NULL,
    code       VARCHAR(10)  NOT NULL UNIQUE,
    country    VARCHAR(100) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Flights  (journey container)
-- ============================================================
CREATE TABLE flights (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    flight_type VARCHAR(20) NOT NULL,
    is_booked   BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flights_user_id     ON flights(user_id);
CREATE INDEX idx_flights_user_booked ON flights(user_id, is_booked, is_deleted);

-- ============================================================
-- Flight Legs  (individual segments of a journey)
-- ============================================================
-- ONE_WAY    → 1 leg  (leg_order = 1)
-- ROUND_TRIP → 2 legs (leg_order = 1 outbound, 2 return)
-- The return leg may depart from a different airport than
-- the outbound leg arrived at.
-- ============================================================
CREATE TABLE flight_legs (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id       UUID      NOT NULL REFERENCES flights(id),
    leg_order       INT       NOT NULL,
    src_airport_id  UUID      NOT NULL REFERENCES airports(id),
    dest_airport_id UUID      NOT NULL REFERENCES airports(id),
    departure_date  DATE      NOT NULL,
    departure_time  TIME      NOT NULL,
    arrival_date    DATE      NOT NULL,
    arrival_time    TIME      NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_flight_leg UNIQUE (flight_id, leg_order)
);

CREATE INDEX idx_flight_legs_flight_id ON flight_legs(flight_id);
