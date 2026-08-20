ALTER TABLE alerts
    ADD COLUMN reopened_at DATETIME(6) NULL AFTER resolution_comment,
    ADD COLUMN reopened_by_user_id BIGINT NULL AFTER reopened_at,
    ADD CONSTRAINT fk_alerts_reopened_user
        FOREIGN KEY (reopened_by_user_id)
            REFERENCES users(id)
            ON DELETE SET NULL;

CREATE INDEX idx_alerts_reopened_user
    ON alerts(reopened_by_user_id);
