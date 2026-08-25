CREATE TABLE monitoring_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    started_at DATETIME(6),
    ended_at DATETIME(6),
    created_by_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    active_room_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN room_id ELSE NULL END) STORED,
    CONSTRAINT fk_monitoring_sessions_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_monitoring_sessions_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_monitoring_session_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_monitoring_session_times CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at),
    CONSTRAINT uq_monitoring_session_active_room UNIQUE (active_room_id),
    INDEX idx_monitoring_sessions_room_created (room_id, created_at),
    INDEX idx_monitoring_sessions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE session_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    occurred_at DATETIME(6) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_session_events_session FOREIGN KEY (session_id) REFERENCES monitoring_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_events_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_session_event_category CHECK (category IN (
        'OBSERVATION', 'INTERVENTION', 'CONFIGURATION_CHANGE', 'MAINTENANCE', 'INCIDENT', 'OTHER'
    )),
    INDEX idx_session_events_session_time (session_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
