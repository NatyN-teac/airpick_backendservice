-- ============================================================
-- Roles
-- ============================================================
CREATE TABLE roles (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(50)  NOT NULL UNIQUE
);

-- ============================================================
-- Modes
-- ============================================================
CREATE TABLE modes (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT
);

-- ============================================================
-- Users
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255),
    provider_id     VARCHAR(255),
    role_id         BIGINT          NOT NULL REFERENCES roles(id),
    active_mode_id  BIGINT          REFERENCES modes(id),
    is_active_user  BOOLEAN         NOT NULL DEFAULT TRUE,
    is_blocked      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- User Profiles
-- ============================================================
CREATE TABLE user_profiles (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL UNIQUE REFERENCES users(id),
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
    id                          BIGSERIAL   PRIMARY KEY,
    user_profile_id             BIGINT      NOT NULL UNIQUE REFERENCES user_profiles(id),
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
    ('DRIVER',    'User operating as a driver'),
    ('PASSENGER', 'User operating as a passenger');

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
