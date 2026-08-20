UPDATE alerts
SET resolution_outcome = 'FIXED'
WHERE resolution_outcome = 'CONFIRMED';

UPDATE alerts
SET resolution_outcome = NULL
WHERE resolution_outcome = 'REQUIRES_INSPECTION';

ALTER TABLE alerts
    DROP CHECK chk_alert_resolution_outcome,
    ADD CONSTRAINT chk_alert_resolution_outcome
        CHECK (resolution_outcome IS NULL OR resolution_outcome IN ('FIXED', 'FALSE_ALARM'));

CREATE TABLE alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    performed_by_user_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    resolution_outcome VARCHAR(30),
    comment VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_alert_history_alert
        FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_history_user
        FOREIGN KEY (performed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_alert_history_action
        CHECK (action IN ('RESOLVED', 'REOPENED')),
    CONSTRAINT chk_alert_history_outcome
        CHECK (resolution_outcome IS NULL OR resolution_outcome IN ('FIXED', 'FALSE_ALARM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alert_history_alert_created_at
    ON alert_history(alert_id, created_at DESC);
