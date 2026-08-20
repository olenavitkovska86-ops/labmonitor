ALTER TABLE alerts
    ADD COLUMN violation_started_at DATETIME(6) NULL AFTER reopened_by_user_id,
    ADD COLUMN initial_value DECIMAL(12,3) NULL AFTER violation_started_at,
    ADD COLUMN latest_value DECIMAL(12,3) NULL AFTER initial_value,
    ADD COLUMN most_extreme_value DECIMAL(12,3) NULL AFTER latest_value,
    ADD COLUMN last_violation_at DATETIME(6) NULL AFTER most_extreme_value,
    ADD COLUMN recovered_at DATETIME(6) NULL AFTER last_violation_at;
