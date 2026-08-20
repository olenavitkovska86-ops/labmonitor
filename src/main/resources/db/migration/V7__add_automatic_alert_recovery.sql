ALTER TABLE alerts
    DROP CHECK chk_alert_resolution_outcome,
    ADD CONSTRAINT chk_alert_resolution_outcome
        CHECK (resolution_outcome IS NULL OR resolution_outcome IN (
            'FIXED', 'FALSE_ALARM', 'AUTO_RECOVERED'
        ));

ALTER TABLE alert_history
    MODIFY performed_by_user_id BIGINT NULL,
    DROP CHECK chk_alert_history_action,
    DROP CHECK chk_alert_history_outcome,
    ADD CONSTRAINT chk_alert_history_action
        CHECK (action IN ('RESOLVED', 'REOPENED', 'AUTO_RECOVERED')),
    ADD CONSTRAINT chk_alert_history_outcome
        CHECK (resolution_outcome IS NULL OR resolution_outcome IN (
            'FIXED', 'FALSE_ALARM', 'AUTO_RECOVERED'
        ));
