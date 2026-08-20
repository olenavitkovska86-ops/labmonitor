CREATE INDEX idx_alerts_sensor_active_violation
    ON alerts(sensor_id, type, status, recovered_at);
