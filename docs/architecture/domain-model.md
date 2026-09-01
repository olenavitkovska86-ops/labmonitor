# LabMonitor Domain Model

This document is a high-level view of the LabMonitor domain. Flyway migrations
under `src/main/resources/db/migration` are the source of truth for table
columns, constraints, and indexes.

## Main Domain Structure

```text
Organization
└── Lab
    └── Room
        ├── Sensor
        │   └── SensorReading
        ├── Device
        │   └── DeviceCredential
        ├── MonitoringSession
        │   └── SessionEvent
        ├── Camera
        │   └── CameraEvent
        ├── EnergyReading
        ├── SecurityState
        └── Alert

User
├── Membership ──> Organization
├── RefreshToken
├── acknowledged Alerts
├── resolved Alerts
└── AuditLog
```

## Entity Status

Status values:

- `IMPLEMENTED` — implemented in Java and ready for use.
- `IN PROGRESS` — implementation has started but is not complete yet.
- `PLANNED` — present in the schema or project plan but not implemented in Java.

| Entity | Purpose | Parent or related entity | Status |
|---|---|---|---|
| `Organization` | Represents a laboratory organization or center | — | IMPLEMENTED |
| `Lab` | Represents a laboratory managed by an organization | `Organization` | IMPLEMENTED |
| `Room` | Represents a physical room inside a lab | `Lab` | IMPLEMENTED |
| `Sensor` | Represents a monitoring device installed in a room | `Room` | IMPLEMENTED |
| `SensorReading` | Stores one measured value from a sensor | `Sensor`, `Room` | IMPLEMENTED |
| `User` | Stores account and profile information | — | IMPLEMENTED |
| `Membership` | Assigns a user and role to an organization | `User`, `Organization` | IMPLEMENTED |
| `RefreshToken` | Stores rotating authenticated-session tokens as hashes | `User` | IMPLEMENTED |
| `Device` | Represents a room-scoped HTTP or browser data client | `Room` | IMPLEMENTED |
| `DeviceCredential` | Stores a revocable BCrypt hash used for device authentication | `Device` | IMPLEMENTED |
| `Camera` | Represents a camera installed in a room | `Room` | PLANNED |
| `CameraEvent` | Stores events reported by a camera | `Camera`, `Room` | PLANNED |
| `EnergyReading` | Stores room-level power and energy measurements | `Room` | PLANNED |
| `SecurityState` | Stores the current security state of a room | `Room` | PLANNED |
| `Alert` | Represents currently implemented sensor threshold and offline incidents | `Room`, optionally `Sensor` and workflow users | IMPLEMENTED |
| `AlertHistory` | Records acknowledgement, resolution, reopening, and automatic recovery | `Alert`, optionally `User` | IMPLEMENTED |
| `MonitoringSession` | Represents a bounded observation period in one room | `Room`, `User` | IMPLEMENTED |
| `SessionEvent` | Records a timestamped user observation or action | `MonitoringSession`, `User` | IMPLEMENTED |
| `AuditLog` | Records administrative and security actions | optionally `Organization`, `User`, `Lab`, `Room` | PLANNED |

## Implemented Relationships

### Organization to Lab

- One organization can contain many labs.
- Each lab belongs to one organization.
- Deleting an organization removes its labs according to the database rules.

### Lab to Room

- One lab can contain many rooms.
- Each room belongs to one lab.
- A room has a type, floor, area, and active state.
- A room can be activated only when its lab is active.
- Deactivating a lab does not overwrite the active state of its rooms or sensors.

### Room to Sensor

- One room can contain many sensors.
- Each sensor belongs to one room.
- A sensor has a type, unit, device status, safe range, and active state.
- A sensor is operational only when the sensor, its room, and its lab are active.
- New sensors cannot be created in an inactive room or lab.
- A sensor cannot be activated or accept new readings while its room or lab is
  inactive.
- Deactivating a room does not overwrite the active state of its sensors.

### Sensor to SensorReading

- One sensor can have many readings.
- Each reading stores a numeric value and measurement time.
- The newest measurement is exposed as the current reading; reading ID
  descending breaks ties between equal measurement timestamps.
- Reading history uses the same deterministic ordering.
- A new reading changes an active sensor status to `ONLINE` and updates
  `lastSeenAt`.
- Late readings remain in history but update threshold-alert state only when
  they are current in measurement order. Every received packet still proves
  liveness and can close an offline alert.

### SensorReading to Alert

- A reading outside the sensor safe range creates a `SENSOR_THRESHOLD` alert.
- Unsafe readings in one continuous violation update the same threshold alert.
  A later violation creates a new alert after recovery, even when an earlier
  high-severity alert remains open for human review.
- Severity is calculated from the deviation relative to the safe-range width:
  `LOW` up to 5%, `MEDIUM` up to 15%, `HIGH` up to 30%, and `CRITICAL`
  above 30%.
- Alerts follow the lifecycle `ACTIVE` -> `ACKNOWLEDGED` -> `RESOLVED`.
- Physical condition (`ONGOING` or recovered) is independent of that workflow
  lifecycle; timeline and export use the physical interval for overlap.
- Alert listing supports hierarchy, status, and severity filters.
- Alert acknowledgement and resolution store the ID of the authenticated user
  obtained from the JWT security context.
- Recovered short `LOW` and `MEDIUM` violations can close automatically.
- Active sensors that stop reporting create a `SENSOR_OFFLINE` alert; the next
  reading restores the sensor and closes that alert automatically.
- A resolved alert can be reopened with a reason, and all lifecycle actions are
  retained in `AlertHistory`.
- Each reading snapshots the safe minimum, safe maximum, and resulting status so
  historical exports remain correct after sensor settings change.

### Room, Device, and Sensor channels

- Each device belongs to one room and has a type, operational status, and
  `lastSeenAt` timestamp.
- A device channel maps a device-scoped `channelKey` to one sensor in the same
  room. A super-admin can assign an existing sensor or create a sensor and
  channel together.
- A device authenticates with a provisioned credential. Only the BCrypt hash is
  persisted; the raw token is returned once when provisioned or rotated.
- Device ingestion requires an active device and credential. Repeating the same
  `messageId` for one device is idempotent and returns the existing reading.
- Device readings retain `sourceDevice` and `messageId` as transport provenance,
  but browser and device readings share the same validation, liveness, threshold,
  storage, and alert processing in `SensorReadingService`.

### User to RefreshToken

- Login creates a short-lived access JWT and a rotating opaque refresh token.
- Only the SHA-256 refresh-token hash is stored. Rotation replaces the token;
  logout and password change revoke applicable token families.

### Room to MonitoringSession

- A room can have many sessions over time but only one `ACTIVE` session at once.
- A session follows `PLANNED` -> `ACTIVE` -> `COMPLETED`; a planned or active
  session may instead become `CANCELLED`.
- Starting a session requires an active room and parent lab.
- Completed and cancelled states are final.

### MonitoringSession to SessionEvent

- Events can be added only while their session is active.
- An event time cannot precede the session start or be in the future.
- Categories are `OBSERVATION`, `INTERVENTION`, `CONFIGURATION_CHANGE`,
  `MAINTENANCE`, `INCIDENT`, and `OTHER`.
- The event author is taken from the authenticated user.

### Users and organizations

- A user can belong to organizations through memberships.
- A membership contains the organization-specific role.
- A membership can cover the whole organization or selected labs and rooms.
- A scoped `LAB_ADMIN` can manage team access only where the target user's
  access intersects the administrator's own scope.
- `SUPER_ADMIN` is a global role; `LAB_ADMIN` and `LIMITED_EMPLOYEE` are
  organization roles.

## Planned Relationships

### Cameras

- A room can contain many cameras.
- A camera can produce many camera events.
- Camera events may generate alerts.

### Audit logs

- Audit records may reference an organization, user, lab, or room.
- They store the performed action and optional JSON details.

## Current Backlog

- Add a small Python analysis example.
- Prepare sample data and screenshots for demonstration.
- Define a reading-retention policy.

Cameras, AI, audit logs, MQTT, energy readings, and security state are outside
the current MVP. A reading-retention policy is useful operational follow-up but
is not required for the core demonstration flow.

## Important Backlog: Alert Rules Configuration

Per-sensor alert rules are intentionally deferred from the base Alerts feature.
The future block should allow `LAB_ADMIN` and `SUPER_ADMIN` users to configure:

- whether alert generation is enabled for a sensor;
- individual `LOW`, `MEDIUM`, and `HIGH` deviation thresholds;
- a cooldown period for repeated alerts;
- restoration of system default thresholds;
- audit logging for every rule change.

Until this block is implemented, the application uses the system defaults
5%, 15%, and 30%. Sensors with only one safe boundary use `HIGH` as a fallback.

## Related Documentation

- Database migrations: `src/main/resources/db/migration`
- User stories: `docs/user-stories.md`
- Roles and permissions: `docs/roles-and-permissions.md`
- Data ingestion: `docs/architecture/data-ingestion.md`
- Alert API examples: `docs/http/alerts.http`
- Menu sketch: `docs/menu-sketch.md`
