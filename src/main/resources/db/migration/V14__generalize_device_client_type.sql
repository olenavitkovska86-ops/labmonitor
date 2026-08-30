ALTER TABLE devices
    DROP CHECK chk_devices_type;

UPDATE devices
SET type = 'DATA_CLIENT'
WHERE type = 'IPHONE';

ALTER TABLE devices
    ADD CONSTRAINT chk_devices_type CHECK (type IN (
        'DATA_CLIENT', 'BROWSER_SIMULATOR', 'CLI_SIMULATOR', 'PHYSICAL_SENSOR', 'GATEWAY'
    ));
