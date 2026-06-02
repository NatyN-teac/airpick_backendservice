-- ============================================================
-- Roles
-- ============================================================
CREATE TABLE roles (
    id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(50)  NOT NULL UNIQUE
);

-- ============================================================
-- Modes
-- ============================================================
CREATE TABLE modes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT
);

-- ============================================================
-- Users
-- ============================================================
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255),
    provider_id     VARCHAR(255),
    role_id         UUID            NOT NULL REFERENCES roles(id),
    active_mode_id  UUID            REFERENCES modes(id),
    is_active_user  BOOLEAN         NOT NULL DEFAULT TRUE,
    is_blocked      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- User Profiles
-- ============================================================
CREATE TABLE user_profiles (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL UNIQUE REFERENCES users(id),
    first_name          VARCHAR(100),
    middle_name         VARCHAR(100),
    last_name           VARCHAR(100),
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100),
    profile_picture_url VARCHAR(500),
    bio                 TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- User Verifications
-- ============================================================
CREATE TABLE user_verifications (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id             UUID        NOT NULL UNIQUE REFERENCES user_profiles(id),
    verification_requested_at   TIMESTAMP,
    status                      VARCHAR(50),
    verification_message        TEXT,
    verified_at                 TIMESTAMP,
    is_denied                   BOOLEAN     NOT NULL DEFAULT FALSE
);

-- ============================================================
-- Seed: Roles
-- ============================================================
INSERT INTO roles (name) VALUES
    ('SYS_ROLE'),
    ('CUSTOMER');

-- ============================================================
-- Seed: Modes
-- ============================================================
INSERT INTO modes (name, description) VALUES
    ('CARRIER', 'User operating as a carrier (traveler carrying items)'),
    ('SHIPPER', 'User operating as a shipper (sender dispatching items)');

-- ============================================================
-- Seed: Admin user  (password will be configured separately)
-- ============================================================
INSERT INTO users (email, password, role_id, is_active_user, is_blocked)
VALUES (
    'admin@airpick.com',
    NULL,
    (SELECT id FROM roles WHERE name = 'SYS_ROLE'),
    TRUE,
    FALSE
);
