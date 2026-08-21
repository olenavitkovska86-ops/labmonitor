ALTER TABLE sensor_readings
    ADD COLUMN safe_min DECIMAL(12,3) NULL AFTER value,
    ADD COLUMN safe_max DECIMAL(12,3) NULL AFTER safe_min,
    ADD COLUMN status VARCHAR(30) NULL AFTER safe_max;

UPDATE sensor_readings reading
    JOIN sensors sensor ON sensor.id = reading.sensor_id
SET reading.safe_min = sensor.min_safe_value,
    reading.safe_max = sensor.max_safe_value,
    reading.status = CASE
        WHEN (sensor.min_safe_value IS NOT NULL AND reading.value < sensor.min_safe_value)
          OR (sensor.max_safe_value IS NOT NULL AND reading.value > sensor.max_safe_value)
        THEN 'OUTSIDE_RANGE'
        ELSE 'SAFE'
    END;

ALTER TABLE sensor_readings
    MODIFY status VARCHAR(30) NOT NULL,
    ADD CONSTRAINT chk_sensor_reading_status
        CHECK (status IN ('SAFE', 'OUTSIDE_RANGE'));
