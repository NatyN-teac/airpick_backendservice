-- ============================================================
-- User Devices  (multi-device FCM token management)
-- ============================================================
CREATE TABLE user_devices (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    fcm_token   VARCHAR(512) NOT NULL,
    platform    VARCHAR(10)  NOT NULL,
    device_name VARCHAR(100),
    last_active TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_devices_fcm_token UNIQUE (fcm_token)
);

CREATE INDEX idx_user_devices_user ON user_devices(user_id);

-- ============================================================
-- Notifications  (persisted inbox, drives FCM push)
-- ============================================================
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID         NOT NULL REFERENCES users(id),
    type         VARCHAR(40)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    ref_type     VARCHAR(30),
    ref_id       UUID,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient          ON notifications(recipient_id);
CREATE INDEX idx_notifications_recipient_unread   ON notifications(recipient_id, is_read) WHERE is_read = FALSE;
