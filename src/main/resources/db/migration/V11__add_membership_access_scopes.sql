ALTER TABLE memberships
    ADD COLUMN scope_type VARCHAR(30) NOT NULL DEFAULT 'ORGANIZATION';

ALTER TABLE memberships
    ADD CONSTRAINT chk_membership_scope_type
        CHECK (scope_type IN ('ORGANIZATION', 'SPECIFIC'));

CREATE TABLE membership_lab_access (
    membership_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    PRIMARY KEY (membership_id, lab_id),
    CONSTRAINT fk_membership_lab_access_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_lab_access_lab
        FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_room_access (
    membership_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    PRIMARY KEY (membership_id, room_id),
    CONSTRAINT fk_membership_room_access_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_room_access_room
        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
