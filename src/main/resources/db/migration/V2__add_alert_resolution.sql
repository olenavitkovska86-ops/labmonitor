ALTER TABLE alerts
    ADD COLUMN resolution_outcome VARCHAR(30) NULL AFTER resolved_by_user_id,
    ADD COLUMN resolution_comment VARCHAR(1000) NULL AFTER resolution_outcome,
    ADD CONSTRAINT chk_alert_resolution_outcome
        CHECK (resolution_outcome IS NULL OR resolution_outcome IN (
            'CONFIRMED', 'FALSE_ALARM', 'REQUIRES_INSPECTION'
        ));
