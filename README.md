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

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your IDE run configuration or terminal environment if your local settings are different from the defaults.

4. The application runs on:

```text
http://localhost:8080
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

The current SQL schema is stored in:

```text
src/main/resources/db/manual/V1__manual_labmonitor_schema.sql
```

## Documentation

Project planning documents are stored in:

```text
docs/menu-sketch.md
docs/roles-and-permissions.md
docs/user-stories.md
```
