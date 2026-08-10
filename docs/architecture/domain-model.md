# LabMonitor Domain Model

This document is a high-level plan of the LabMonitor domain. The manual database
schema remains the source of truth for table columns, constraints, and indexes.

## Main Domain Structure

```text
Organization
└── Lab
    └── Room
        ├── Sensor
        │   └── SensorReading
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
| `User` | Stores account and profile information | — | PLANNED |
| `Membership` | Assigns a user and role to an organization | `User`, `Organization` | PLANNED |
| `RefreshToken` | Supports future authenticated sessions | `User` | PLANNED |
| `Camera` | Represents a camera installed in a room | `Room` | PLANNED |
| `CameraEvent` | Stores events reported by a camera | `Camera`, `Room` | PLANNED |
| `EnergyReading` | Stores room-level power and energy measurements | `Room` | PLANNED |
| `SecurityState` | Stores the current security state of a room | `Room` | PLANNED |
| `Alert` | Represents sensor, camera, security, energy, or system incidents | `Room`, optionally `Sensor`, `Camera`, `User` | PLANNED |
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
- The newest measurement is exposed as the current reading.
- Reading history is ordered by measurement time from newest to oldest.
- A new reading changes an active sensor status to `ONLINE` and updates
  `lastSeenAt`.

## Planned Relationships

### Users and organizations

- A user can belong to organizations through memberships.
- A membership contains the organization-specific role.
- `SUPER_ADMIN` is a global role; `LAB_ADMIN` and `LIMITED_EMPLOYEE` are
  organization roles.

### Cameras

- A room can contain many cameras.
- A camera can produce many camera events.
- Camera events may generate alerts.

### Alerts

- Every alert belongs to a room.
- An alert may reference a sensor or camera that caused it.
- Users may acknowledge and resolve alerts.

### Audit logs

- Audit records may reference an organization, user, lab, or room.
- They store the performed action and optional JSON details.

## Recommended Implementation Order

1. Detect sensor readings outside the configured safe range.
2. Implement `Alert` creation, listing, acknowledgement, and resolution.
3. Implement `User`, `Membership`, authentication, and role-based access.
4. Implement `Camera` and `CameraEvent`.
5. Implement `AuditLog`.
6. Implement `EnergyReading` and `SecurityState` if they remain in the final
   project scope.
7. Add analytics and Power BI after enough historical data is available.

## Related Documentation

- Database schema: `src/main/resources/db/manual/V1__manual_labmonitor_schema.sql`
- User stories: `docs/user-stories.md`
- Roles and permissions: `docs/roles-and-permissions.md`
- Data ingestion: `docs/architecture/data-ingestion.md`
- Menu sketch: `docs/menu-sketch.md`
