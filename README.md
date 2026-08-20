# LabMonitor

LabMonitor is a Spring Boot project for managing and monitoring laboratory organizations, labs, rooms, sensors, cameras, alerts, and audit logs.

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

Open the web interface in a browser:

```text
http://localhost:8080
http://localhost:8080/organizations.html
http://localhost:8080/labs.html
http://localhost:8080/rooms.html
http://localhost:8080/sensors.html
http://localhost:8080/sensor-readings.html?sensorId=1
http://localhost:8080/analytics.html
```

The root URL opens the LabMonitor home page. The web interface follows the
laboratory structure:

```text
Organizations -> Labs -> Rooms -> Sensors
```

The pages provide viewing, searching, creating, editing, activation, and
deactivation where supported. The sensor page also provides safe value range
configuration. Select a sensor name to view its current reading and measurement
history.

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
http://localhost:8080/api/analytics/organizations/1/overview
http://localhost:8080/api/analytics/organizations/1/problem-rooms
```

If the corresponding database table is empty, an API endpoint returns an empty
JSON array:

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

## Documentation

Project planning documents are stored in:

```text
docs/menu-sketch.md
docs/roles-and-permissions.md
docs/user-stories.md
docs/architecture/domain-model.md
docs/http/sensors.http
docs/http/sensor-readings.http
```
