# LabMonitor

LabMonitor is a Spring Boot application for monitoring controlled spaces such as
laboratories and server rooms. It manages organizations, labs, rooms, sensors,
readings, alerts, analytics, and timestamped monitoring sessions. Cameras and
audit logs are outside the current MVP and remain possible future work.

## Requirements

- Java 21
- Docker Desktop

## How to run

1. Copy `.env.example` to `.env` and adjust values if needed.

The local `.env` file is loaded by Spring Boot and is ignored by Git. It must
contain a unique `JWT_SECRET` of at least 32 bytes; never reuse the example value.

2. Start MySQL with Docker Compose:

```bash
docker compose up -d
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

The project uses MySQL on `localhost:3306`. Use your local database settings, for example:

```text
DB_URL=jdbc:mysql://localhost:3306/labmonitor_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_local_password
```

Monitoring rules have documented defaults in `application.properties` and can
be changed with environment variables such as `ALERT_AUTO_RECOVERY_MAX_DURATION`
or `READING_HISTORY_MAX_RESULTS`, without rebuilding the application.

For a production deployment, activate the `prod` profile and provide
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` through the deployment
environment or secret manager:

```text
SPRING_PROFILES_ACTIVE=prod
```

The production profile requires explicit database settings, enables secure JWT
cookies, honors reverse-proxy forwarding headers, disables SQL logging, and
does not baseline an unknown schema.

For a brand-new empty database only, the first super-admin may be created by
setting `BOOTSTRAP_ADMIN_ENABLED=true` together with
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`. Disable bootstrap again
immediately after the account is created. It is disabled by default.

## Sensor data clients

A `SUPER_ADMIN` can open a data client for an individual sensor from the
Sensors page and submit generated numeric measurements every five seconds or
every minute. The page must remain open while readings are being sent.

An experimental motion client is available from `MOTION` sensors, explicitly
named motion sensors (which may use the generic `OTHER` type), and channels of
registered `DATA_CLIENT` devices. It uses browser motion data and calculates RMS
acceleration over 500 ms. A sensor-launched client submits through the standard
user-authenticated reading endpoint. A device-launched client accepts the
one-time provisioned credential and submits through device ingestion. The token
is held only in the open browser tab. Motion access requires opening the
application over HTTPS on a mobile device.

Both pages are ordinary API clients. The backend does not classify readings as
generated, virtual, or physical: every accepted value follows the same
`SensorReading` validation, storage, liveness, threshold, and alert flow.

## Device ingestion

Device ingestion can be configured by a `SUPER_ADMIN` from the **Devices** page.
Existing browser data clients and demonstrations do not need to switch to device
credentials; the current user-authenticated sensor ingestion flow remains unchanged.

A `SUPER_ADMIN` can register a device, assign its channel keys to sensors, and
provision a device credential. The raw credential is returned only when it is
provisioned or rotated; only its BCrypt hash is stored. Devices submit readings
with `Authorization: Device <token>` to:

```text
POST /api/device/readings
```

The request contains `channel`, `value`, `measuredAt`, and a device-scoped
`messageId`. Repeating the same `messageId` for one device returns the original
reading as `already_processed` instead of storing a duplicate. User-authenticated
ingestion through `POST /api/sensor-readings` remains available.

Device administration endpoints are restricted to `SUPER_ADMIN`:

```text
POST /api/devices
GET  /api/devices
GET  /api/devices/{deviceId}
PATCH /api/devices/{deviceId}/status
PUT  /api/devices/{deviceId}/sensors/{sensorId}
POST /api/devices/{deviceId}/sensor-channels
GET  /api/devices/{deviceId}/channels
DELETE /api/devices/{deviceId}/sensors/{sensorId}
GET  /api/devices/{deviceId}/credentials
POST /api/devices/{deviceId}/credentials/provision
POST /api/devices/{deviceId}/credentials/rotate
POST /api/devices/{deviceId}/credentials/{credentialId}/revoke
```

Active sensors are checked for missing readings every 10 seconds. A sensor that
has not reported for two minutes is marked offline and creates one
`SENSOR_OFFLINE` alert. A new reading restores the sensor and closes that alert
automatically. The durations can be changed with `SENSOR_OFFLINE_AFTER` and
`SENSOR_OFFLINE_CHECK_INTERVAL`.

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your IDE run configuration or terminal environment if your local settings are different from the defaults.

4. The application runs on port `8080`.

Open `/login.html` and sign in before using the protected API or application
pages. API authentication uses the JWT issued by `/auth/login`.
The browser receives a 15-minute access JWT in the `LABMONITOR_SESSION` HttpOnly
cookie and a rotating 14-day opaque refresh token in `LABMONITOR_REFRESH`. The
refresh token is stored only as a SHA-256 hash in the database, is replaced by
`POST /auth/refresh`, and is revoked on logout or password change. The frontend
automatically refreshes once and retries the original request after a `401`.

Open the web interface in a browser:

```text
http://localhost:8080
http://localhost:8080/login.html
http://localhost:8080/organizations.html
http://localhost:8080/labs.html
http://localhost:8080/rooms.html
http://localhost:8080/sensors.html
http://localhost:8080/sensor-readings.html?sensorId=1
http://localhost:8080/analytics.html
http://localhost:8080/alerts.html
http://localhost:8080/monitoring-sessions.html
http://localhost:8080/devices.html
http://localhost:8080/profile.html
```

The root URL opens the LabMonitor home page. The web interface follows the
laboratory structure:

```text
Organizations -> Labs -> Rooms -> Sensors
```

The pages provide viewing, searching, creating, editing, activation, and
deactivation where supported. The sensor page also provides safe value range
configuration. Select a sensor name to view its current reading and measurement
history or export it as CSV. The sensors page can export readings for every
sensor in a selected room. Both exports use a long, analysis-friendly format
suitable for Python, Excel, R, and MATLAB.

Activity follows the laboratory hierarchy. A sensor can accept readings only
when the sensor, its room, and its lab are all active. Deactivating a lab or room
does not change the saved active state of its child rooms or sensors. This allows
previously active children to become operational again when their parent is
reactivated, while individually deactivated sensors remain inactive.

The implemented REST API endpoints are also available directly:

```text
http://localhost:8080/api/organizations
http://localhost:8080/api/labs
http://localhost:8080/api/rooms
http://localhost:8080/api/sensors
http://localhost:8080/api/sensors/1/current-reading
http://localhost:8080/api/sensors/1/readings
http://localhost:8080/api/sensor-readings/export?roomId=1&sensorId=1&from=2026-08-24T00:00:00&to=2026-08-25T00:00:00
http://localhost:8080/api/monitoring-sessions
http://localhost:8080/api/analytics/organizations/1/overview
http://localhost:8080/api/analytics/organizations/1/problem-rooms
```

Authenticated list endpoints return an empty JSON array when no matching data
exists:

```json
[]
```

The examples under `docs/http` assume an authenticated browser/API session.
State-changing user API requests also require the `X-XSRF-TOKEN` header matching
the `XSRF-TOKEN` cookie obtained from `GET /api/csrf`. Device ingestion uses its
own `Authorization: Device <token>` authentication and is not CSRF-protected.

A successful application startup is confirmed by these messages in the log:

```text
Tomcat started on port 8080
Started LabmonitorApplication
```

## Database

The Docker Compose configuration starts MySQL on port `3306`.

Default database settings:

```text
Database: labmonitor_db
Username: root
Password: root_password
Port: 3306
```

Database migrations are applied automatically by Flyway when the application
starts. Migration files are stored in:

```text
src/main/resources/db/migration
```

For an existing database created with the former manual schema, set
`FLYWAY_BASELINE_ON_MIGRATE=true` only for the reviewed, one-time baseline run.
Disable it again afterwards. New databases and databases already managed by
Flyway must keep the default value `false`. The files under
`src/main/resources/db/manual` are retained for reference and should not
normally be run by hand.

## Reading export

`GET /api/sensor-readings/export` exports either one sensor (`sensorId`) or all
sensors in a room. `roomId`, `from`, and `to` are required. Exports are limited
to 30 days and 250,000 rows by default. Override these limits with
`READING_EXPORT_MAX_PERIOD` and `READING_EXPORT_MAX_ROWS`.

Each row includes measurement and receipt times, room and sensor identity,
sensor type, value, unit, the safe-range snapshot, and the calculated status.
Numeric fields, including negative values, remain numeric in the CSV. Formula
protection is applied only to textual fields whose first character could be
interpreted as a spreadsheet formula.

## Monitoring sessions

A monitoring session represents a bounded observation period in one room. A
session can be planned, started, completed, or cancelled. Only one session can
be active in a room at a time. Timestamped events can be recorded while the
session is active using universal categories such as observation, intervention,
maintenance, and incident. The session detail view combines sensor readings,
event markers, and alert markers in a bounded timeline. Long timelines retain
up to 200 readings per sensor, sampled evenly across the full session so that
high-frequency sensors do not hide sparse sensors or earlier periods. The first
and last reading of each sensor are retained when sampling is required.

A started session can also be downloaded as a ZIP containing `session.csv`,
`readings.csv`, `events.csv`, and `alerts.csv`. The files include stable entity
IDs and organization, lab, room, and sensor context where applicable. Readings
include 15 minutes before and after the session, a `context_session_id`, and an
explicit `BEFORE`, `DURING`, or `AFTER` phase. Events use `session_id` because
they belong to the session. Alerts use `context_session_id` because they only
overlap its time window; they expose workflow and physical-condition status
separately, together with start/end phases and `overlaps_session`.

For an active session, `ended_at` remains empty. The current time is used only
as the effective upper boundary for collecting timeline and export data.
Cancelled sessions that were never started have neither a timeline nor an
export. Numeric CSV fields remain numeric, while potentially formula-like user
text is escaped for spreadsheet safety.

## Documentation

Project planning documents are stored in:

```text
docs/menu-sketch.md
docs/roles-and-permissions.md
docs/user-stories.md
docs/architecture/domain-model.md
docs/architecture/data-ingestion.md
docs/analytics-mvp.md
docs/http/auth.http
docs/http/organizations.http
docs/http/labs.http
docs/http/rooms.http
docs/http/sensors.http
docs/http/sensor-readings.http
docs/http/monitoring-sessions.http
docs/http/alerts.http
docs/http/analytics.http
docs/http/user.http
docs/http/access.http
docs/http/devices.http
```
