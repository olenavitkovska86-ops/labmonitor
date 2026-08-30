CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_devices_organization FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT chk_devices_type CHECK (type IN (
        'IPHONE', 'BROWSER_SIMULATOR', 'CLI_SIMULATOR', 'PHYSICAL_SENSOR', 'GATEWAY'
    )),
    CONSTRAINT chk_devices_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE device_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    credential_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    issued_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6),
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_device_credentials_device FOREIGN KEY (device_id)
        REFERENCES devices(id) ON DELETE CASCADE,
    CONSTRAINT chk_device_credentials_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    INDEX idx_device_credentials_device_status (device_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sensors
    ADD COLUMN device_id BIGINT NULL AFTER room_id,
    ADD COLUMN channel_key VARCHAR(100) NULL AFTER device_id,
    ADD CONSTRAINT fk_sensors_device FOREIGN KEY (device_id)
        REFERENCES devices(id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_sensors_device_channel UNIQUE (device_id, channel_key);

ALTER TABLE sensor_readings
    ADD COLUMN source_device_id BIGINT NULL AFTER room_id,
    ADD COLUMN message_id VARCHAR(100) NULL AFTER source_device_id,
    ADD CONSTRAINT fk_sensor_readings_source_device FOREIGN KEY (source_device_id)
        REFERENCES devices(id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_sensor_readings_device_message UNIQUE (source_device_id, message_id);
