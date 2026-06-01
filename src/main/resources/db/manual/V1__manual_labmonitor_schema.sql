USE labmonitor_db;

-- ============================================================
-- LabMonitor / Smart Lab Digital Twin API
-- Manual MySQL 8+ schema
-- ============================================================

-- ============================================================
-- 1. ORGANIZATIONS
-- ============================================================

CREATE TABLE organizations (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               name VARCHAR(150) NOT NULL,
                               description VARCHAR(500),
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. USERS
-- ============================================================

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       phone VARCHAR(50),
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       global_role VARCHAR(30) NOT NULL DEFAULT 'NONE',
                       last_login_at DATETIME(6),
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT chk_users_status
                           CHECK (status IN ('ACTIVE', 'INVITED', 'DISABLED')),

                       CONSTRAINT chk_users_global_role
                           CHECK (global_role IN ('NONE', 'SUPER_ADMIN')),

                       CONSTRAINT chk_users_email
                           CHECK (email LIKE '%@%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 3. MEMBERSHIPS
-- User role inside organization
-- Roles: LAB_ADMIN / LIMITED_EMPLOYEE
-- ============================================================

CREATE TABLE memberships (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             organization_id BIGINT NOT NULL,
                             user_id BIGINT NOT NULL,
                             role VARCHAR(30) NOT NULL,
                             created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                             CONSTRAINT fk_memberships_organization
                                 FOREIGN KEY (organization_id)
                                     REFERENCES organizations(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_memberships_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT chk_membership_role
                                 CHECK (role IN ('LAB_ADMIN', 'LIMITED_EMPLOYEE')),

                             CONSTRAINT uq_membership_org_user
                                 UNIQUE (organization_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. REFRESH TOKENS
-- For future JWT / refresh token support
-- ============================================================

CREATE TABLE refresh_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6),
                                user_agent VARCHAR(500),
                                ip_address VARCHAR(45),
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. LABS
-- ============================================================

CREATE TABLE labs (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      organization_id BIGINT NOT NULL,
                      name VARCHAR(150) NOT NULL,
                      location VARCHAR(255),
                      description VARCHAR(500),
                      active BOOLEAN NOT NULL DEFAULT TRUE,
                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                      updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                      CONSTRAINT fk_labs_organization
                          FOREIGN KEY (organization_id)
                              REFERENCES organizations(id)
                              ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 6. ROOMS
-- ============================================================

CREATE TABLE rooms (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       lab_id BIGINT NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       type VARCHAR(50) NOT NULL,
                       floor INT,
                       area_m2 DECIMAL(6,2),
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT fk_rooms_lab
                           FOREIGN KEY (lab_id)
                               REFERENCES labs(id)
                               ON DELETE CASCADE,

                       CONSTRAINT chk_room_type
                           CHECK (type IN (
                                           'EXPERIMENT_ROOM',
                                           'STORAGE_ROOM',
                                           'SERVER_ROOM',
                                           'CLEAN_ROOM',
                                           'OFFICE',
                                           'ENTRANCE',
                                           'OTHER'
                               )),

                       CONSTRAINT chk_room_area
                           CHECK (area_m2 IS NULL OR area_m2 > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 7. CAMERAS
-- ============================================================

CREATE TABLE cameras (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         room_id BIGINT NOT NULL,
                         name VARCHAR(150) NOT NULL,
                         model VARCHAR(100),
                         stream_url VARCHAR(500),
                         ip_address VARCHAR(45),
                         status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
                         recording BOOLEAN NOT NULL DEFAULT FALSE,
                         night_vision_on BOOLEAN NOT NULL DEFAULT FALSE,
                         firmware_version VARCHAR(50),
                         position_description VARCHAR(255),
                         last_seen_at DATETIME(6),
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                         CONSTRAINT fk_cameras_room
                             FOREIGN KEY (room_id)
                                 REFERENCES rooms(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT chk_camera_status
                             CHECK (status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE', 'ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 8. SENSORS
-- ============================================================

CREATE TABLE sensors (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         room_id BIGINT NOT NULL,
                         name VARCHAR(150) NOT NULL,
                         type VARCHAR(50) NOT NULL,
                         unit VARCHAR(30),
                         status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
                         min_safe_value DECIMAL(12,3),
                         max_safe_value DECIMAL(12,3),
                         last_seen_at DATETIME(6),
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                         CONSTRAINT fk_sensors_room
                             FOREIGN KEY (room_id)
                                 REFERENCES rooms(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT chk_sensor_type
                             CHECK (type IN (
                                             'TEMPERATURE',
                                             'HUMIDITY',
                                             'CO2',
                                             'SMOKE',
                                             'MOTION',
                                             'DOOR',
                                             'PRESSURE',
                                             'LIGHT',
                                             'NOISE',
                                             'ENERGY',
                                             'OCCUPANCY',
                                             'OTHER'
                                 )),

                         CONSTRAINT chk_sensor_status
                             CHECK (status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE', 'ERROR')),

                         CONSTRAINT chk_sensor_safe_range
                             CHECK (
                                 min_safe_value IS NULL
                                     OR max_safe_value IS NULL
                                     OR min_safe_value < max_safe_value
                                 )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. SENSOR READINGS
-- One row = one measurement from one sensor
-- ============================================================

CREATE TABLE sensor_readings (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 sensor_id BIGINT NOT NULL,
                                 room_id BIGINT NOT NULL,
                                 value DECIMAL(12,3) NOT NULL,
                                 measured_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 CONSTRAINT fk_sensor_readings_sensor
                                     FOREIGN KEY (sensor_id)
                                         REFERENCES sensors(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_sensor_readings_room
                                     FOREIGN KEY (room_id)
                                         REFERENCES rooms(id)
                                         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 10. ENERGY READINGS
-- Room-level energy analytics
-- ============================================================

CREATE TABLE energy_readings (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 room_id BIGINT NOT NULL,
                                 total_power_kw DECIMAL(12,3) NOT NULL,
                                 total_energy_kwh DECIMAL(12,3),
                                 lighting_power_kw DECIMAL(12,3),
                                 camera_power_kw DECIMAL(12,3),
                                 equipment_power_kw DECIMAL(12,3),
                                 measured_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 CONSTRAINT fk_energy_readings_room
                                     FOREIGN KEY (room_id)
                                         REFERENCES rooms(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_total_power_kw
                                     CHECK (total_power_kw >= 0),

                                 CONSTRAINT chk_total_energy_kwh
                                     CHECK (total_energy_kwh IS NULL OR total_energy_kwh >= 0),

                                 CONSTRAINT chk_lighting_power_kw
                                     CHECK (lighting_power_kw IS NULL OR lighting_power_kw >= 0),

                                 CONSTRAINT chk_camera_power_kw
                                     CHECK (camera_power_kw IS NULL OR camera_power_kw >= 0),

                                 CONSTRAINT chk_equipment_power_kw
                                     CHECK (equipment_power_kw IS NULL OR equipment_power_kw >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 11. SECURITY STATES
-- One current security state per room
-- ============================================================

CREATE TABLE security_states (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 room_id BIGINT NOT NULL UNIQUE,
                                 mode VARCHAR(30) NOT NULL DEFAULT 'DISARMED',
                                 door_locked BOOLEAN NOT NULL DEFAULT FALSE,
                                 alarm_armed BOOLEAN NOT NULL DEFAULT FALSE,
                                 motion_detected BOOLEAN NOT NULL DEFAULT FALSE,
                                 last_changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                                 CONSTRAINT fk_security_states_room
                                     FOREIGN KEY (room_id)
                                         REFERENCES rooms(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_security_mode
                                     CHECK (mode IN ('DISARMED', 'ARMED', 'MAINTENANCE', 'LOCKDOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 12. CAMERA EVENTS
-- ============================================================

CREATE TABLE camera_events (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               camera_id BIGINT NOT NULL,
                               room_id BIGINT NOT NULL,
                               event_type VARCHAR(50) NOT NULL,
                               severity VARCHAR(30) NOT NULL DEFAULT 'INFO',
                               description VARCHAR(500),
                               snapshot_url VARCHAR(500),
                               occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_camera_events_camera
                                   FOREIGN KEY (camera_id)
                                       REFERENCES cameras(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_camera_events_room
                                   FOREIGN KEY (room_id)
                                       REFERENCES rooms(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_camera_event_type
                                   CHECK (event_type IN (
                                                         'MOTION_DETECTED',
                                                         'PERSON_DETECTED',
                                                         'CAMERA_ONLINE',
                                                         'CAMERA_OFFLINE',
                                                         'RECORDING_STARTED',
                                                         'RECORDING_STOPPED',
                                                         'NIGHT_VISION_ENABLED',
                                                         'NIGHT_VISION_DISABLED',
                                                         'PEOPLE_COUNT_UPDATED',
                                                         'OTHER'
                                       )),

                               CONSTRAINT chk_camera_event_severity
                                   CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 13. ALERTS
-- Active alerts, acknowledged alerts, resolved alerts
-- ============================================================

CREATE TABLE alerts (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        room_id BIGINT NOT NULL,
                        sensor_id BIGINT,
                        camera_id BIGINT,

                        type VARCHAR(50) NOT NULL,
                        severity VARCHAR(30) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                        title VARCHAR(150) NOT NULL,
                        message VARCHAR(1000),

                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        acknowledged_at DATETIME(6),
                        acknowledged_by_user_id BIGINT,
                        resolved_at DATETIME(6),
                        resolved_by_user_id BIGINT,

                        CONSTRAINT fk_alerts_room
                            FOREIGN KEY (room_id)
                                REFERENCES rooms(id)
                                ON DELETE CASCADE,

                        CONSTRAINT fk_alerts_sensor
                            FOREIGN KEY (sensor_id)
                                REFERENCES sensors(id)
                                ON DELETE SET NULL,

                        CONSTRAINT fk_alerts_camera
                            FOREIGN KEY (camera_id)
                                REFERENCES cameras(id)
                                ON DELETE SET NULL,

                        CONSTRAINT fk_alerts_ack_user
                            FOREIGN KEY (acknowledged_by_user_id)
                                REFERENCES users(id)
                                ON DELETE SET NULL,

                        CONSTRAINT fk_alerts_resolved_user
                            FOREIGN KEY (resolved_by_user_id)
                                REFERENCES users(id)
                                ON DELETE SET NULL,

                        CONSTRAINT chk_alert_type
                            CHECK (type IN (
                                            'SENSOR_THRESHOLD',
                                            'SENSOR_OFFLINE',
                                            'CAMERA_EVENT',
                                            'CAMERA_OFFLINE',
                                            'SECURITY',
                                            'INTRUSION',
                                            'ENERGY',
                                            'SYSTEM',
                                            'OTHER'
                                )),

                        CONSTRAINT chk_alert_severity
                            CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

                        CONSTRAINT chk_alert_status
                            CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 14. AUDIT LOGS
-- Admin actions, security actions, profile updates, etc.
-- ============================================================

CREATE TABLE audit_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            organization_id BIGINT,
                            user_id BIGINT,
                            lab_id BIGINT,
                            room_id BIGINT,

                            action VARCHAR(100) NOT NULL,
                            entity_type VARCHAR(100),
                            entity_id BIGINT,
                            description VARCHAR(1000),
                            details JSON,
                            ip_address VARCHAR(45),

                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                            CONSTRAINT fk_audit_logs_organization
                                FOREIGN KEY (organization_id)
                                    REFERENCES organizations(id)
                                    ON DELETE SET NULL,

                            CONSTRAINT fk_audit_logs_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE SET NULL,

                            CONSTRAINT fk_audit_logs_lab
                                FOREIGN KEY (lab_id)
                                    REFERENCES labs(id)
                                    ON DELETE SET NULL,

                            CONSTRAINT fk_audit_logs_room
                                FOREIGN KEY (room_id)
                                    REFERENCES rooms(id)
                                    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_status ON users(status);

CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_organization_id ON memberships(organization_id);
CREATE INDEX idx_memberships_role ON memberships(role);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens(user_id, revoked_at);

CREATE INDEX idx_labs_organization_id ON labs(organization_id);
CREATE INDEX idx_labs_active ON labs(active);

CREATE INDEX idx_rooms_lab_id ON rooms(lab_id);
CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_active ON rooms(active);

CREATE INDEX idx_cameras_room_id ON cameras(room_id);
CREATE INDEX idx_cameras_status ON cameras(status);
CREATE INDEX idx_cameras_active ON cameras(active);

CREATE INDEX idx_sensors_room_id ON sensors(room_id);
CREATE INDEX idx_sensors_type ON sensors(type);
CREATE INDEX idx_sensors_status ON sensors(status);
CREATE INDEX idx_sensors_active ON sensors(active);

CREATE INDEX idx_sensor_readings_sensor_measured_at
    ON sensor_readings(sensor_id, measured_at DESC);

CREATE INDEX idx_sensor_readings_room_measured_at
    ON sensor_readings(room_id, measured_at DESC);

CREATE INDEX idx_energy_readings_room_measured_at
    ON energy_readings(room_id, measured_at DESC);

CREATE INDEX idx_camera_events_camera_occurred_at
    ON camera_events(camera_id, occurred_at DESC);

CREATE INDEX idx_camera_events_room_occurred_at
    ON camera_events(room_id, occurred_at DESC);

CREATE INDEX idx_camera_events_type
    ON camera_events(event_type);

CREATE INDEX idx_alerts_room_status_created_at
    ON alerts(room_id, status, created_at DESC);

CREATE INDEX idx_alerts_room_severity_created_at
    ON alerts(room_id, severity, created_at DESC);

CREATE INDEX idx_alerts_status
    ON alerts(status);

CREATE INDEX idx_alerts_severity
    ON alerts(severity);

CREATE INDEX idx_alerts_ack_user
    ON alerts(acknowledged_by_user_id);

CREATE INDEX idx_alerts_resolved_user
    ON alerts(resolved_by_user_id);

CREATE INDEX idx_audit_logs_user_created_at
    ON audit_logs(user_id, created_at DESC);

CREATE INDEX idx_audit_logs_room_created_at
    ON audit_logs(room_id, created_at DESC);

CREATE INDEX idx_audit_logs_lab_created_at
    ON audit_logs(lab_id, created_at DESC);

CREATE INDEX idx_audit_logs_organization_created_at
    ON audit_logs(organization_id, created_at DESC);

CREATE INDEX idx_audit_logs_action
    ON audit_logs(action);


-- ============================================================
-- CHECK
-- ============================================================

SHOW TABLES;
