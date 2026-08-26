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

## Sensor simulator

The local demo simulator is disabled whenever the application starts. A
`SUPER_ADMIN` or `LAB_ADMIN` can start and stop it from the home page and choose
an interval of five seconds or one minute.

Only active sensors with at least one configured safe boundary are simulated.
Even-numbered sensors produce short mild deviations that can auto-recover;
odd-numbered sensors produce escalating violations for the manual alert flow.
The simulator uses the regular reading service and is limited to 20 sensors by
default. It should remain disabled outside local demonstrations.

Active sensors are checked for missing readings every 10 seconds. A sensor that
has not reported for two minutes is marked offline and creates one
`SENSOR_OFFLINE` alert. A new reading restores the sensor and closes that alert
automatically. The durations can be changed with `SENSOR_OFFLINE_AFTER` and
`SENSOR_OFFLINE_CHECK_INTERVAL`.

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your IDE run configuration or terminal environment if your local settings are different from the defaults.

4. The application runs on port `8080`.

Open `/login.html` and sign in before using the protected API or application
pages. Authentication is stateless and uses the JWT returned by `/auth/login`.

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
```

The root URL opens the LabMonitor home page. The web interface follows the
primary workflow navigation:

```text
Overview | Monitor | History & exports | Alerts | Sessions
```

`Monitor` follows the laboratory structure:

```text
Organizations -> Labs -> Rooms -> Sensors
```

Role-specific Administration navigation will be connected after the final
user/role/security model is merged. Navigation visibility is a UX concern;
backend authorization remains responsible for protecting data and actions.

`History & exports` keeps historical analysis out of the operational Overview.
It provides alert periods, clickable daily alert bars, reading CSV export by
room/sensor/time range, and a path to Monitoring Session ZIP exports. The
current Administration responsibility editor uses clearly labelled demo data
until the final membership and assignment APIs are available.

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

For an existing database created with the former manual schema, Flyway records
version 1 as the baseline and applies migrations starting with version 2. Team
members therefore only need to pull the code and start the application. The
files under `src/main/resources/db/manual` are retained for reference and should
not normally be run by hand.

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
docs/http/sensors.http
docs/http/sensor-readings.http
docs/http/monitoring-sessions.http
```
