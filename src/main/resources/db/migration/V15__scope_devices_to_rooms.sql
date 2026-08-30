ALTER TABLE devices
    ADD COLUMN room_id BIGINT NULL AFTER id;

UPDATE devices d
JOIN (
    SELECT device_id, MIN(room_id) AS room_id
    FROM sensors
    WHERE device_id IS NOT NULL
    GROUP BY device_id
    HAVING COUNT(DISTINCT room_id) = 1
) assigned ON assigned.device_id = d.id
SET d.room_id = assigned.room_id;

-- Legacy devices without channels cannot reveal their former room. Assign the
-- first room in their organization deterministically; deployments should audit
-- this mapping before applying the migration.
UPDATE devices d
JOIN (
    SELECT l.organization_id, MIN(r.id) AS room_id
    FROM rooms r
    JOIN labs l ON l.id = r.lab_id
    GROUP BY l.organization_id
) fallback ON fallback.organization_id = d.organization_id
SET d.room_id = fallback.room_id
WHERE d.room_id IS NULL;

ALTER TABLE devices
    ADD CONSTRAINT fk_devices_room FOREIGN KEY (room_id)
        REFERENCES rooms(id) ON DELETE RESTRICT,
    MODIFY COLUMN room_id BIGINT NOT NULL,
    DROP FOREIGN KEY fk_devices_organization,
    DROP COLUMN organization_id;
