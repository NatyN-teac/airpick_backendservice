ALTER TABLE user_verifications
    ADD COLUMN veriff_session_id VARCHAR(255);

CREATE UNIQUE INDEX idx_user_verifications_veriff_session_id
    ON user_verifications (veriff_session_id)
    WHERE veriff_session_id IS NOT NULL;
