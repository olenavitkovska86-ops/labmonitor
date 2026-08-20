ALTER TABLE alert_history
    DROP CHECK chk_alert_history_action,
    ADD CONSTRAINT chk_alert_history_action
        CHECK (action IN ('ACKNOWLEDGED', 'RESOLVED', 'REOPENED', 'AUTO_RECOVERED'));
