ALTER TABLE sensors
    DROP CHECK chk_sensor_type;

ALTER TABLE sensors
    ADD CONSTRAINT chk_sensor_type CHECK (type IN (
        'TEMPERATURE', 'HUMIDITY', 'CO2', 'SMOKE', 'MOTION', 'VIBRATION',
        'DOOR', 'PRESSURE', 'LIGHT', 'NOISE', 'ENERGY', 'OCCUPANCY', 'OTHER'
    ));
