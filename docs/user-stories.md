## User Stories

Unless explicitly marked as future scope, the stories below describe the
current implementation. Cameras and audit logs are future scope. Device HTTP
ingestion, rotating login refresh tokens, scoped memberships, monitoring
sessions, timestamped events, the timeline UI, and ZIP export are implemented.

### 1. Organizations

User story:
As a SUPER_ADMIN,
I want to create an organization,
so that I can register a laboratory center in the system.

Acceptance criteria:
1. SUPER_ADMIN can create an organization with name and description.
2. Organization name is required.
3. Organization name must not be longer than 150 characters.
4. Organization description must not be longer than 500 characters.
5. After creation, system returns id, name, description, createdAt, updatedAt.
6. SUPER_ADMIN can get organization by id.
7. SUPER_ADMIN can view list of organizations.
8. If organization by id does not exist, system returns 404.

User story:
As a SUPER_ADMIN,
I want to update organization information,
so that organization data stays correct and up to date.

Acceptance criteria:
1. SUPER_ADMIN can update organization name.
2. SUPER_ADMIN can update organization description.
3. Organization name is required.
4. Organization name must not be longer than 150 characters.
5. Organization description must not be longer than 500 characters.
6. updatedAt changes after successful update.
7. If organization does not exist, system returns 404.

User story:
As a SUPER_ADMIN,
I want to delete an organization,
so that inactive or incorrect organizations can be removed from the system.

Acceptance criteria:
1. SUPER_ADMIN can delete organization by id.
2. After deletion, organization is no longer available by id.
3. If organization does not exist, system returns 404.
4. Related labs, rooms, sensors, devices, readings, alerts, sessions, and
   memberships are removed according to database rules.

### 2. Users and Roles

User story:
As a SUPER_ADMIN,
I want to create a user,
so that the user can access the system.

Acceptance criteria:
1. SUPER_ADMIN can create a user with email, first name, last name, and password.
2. Email is required.
3. Email must be unique.
4. A newly created user receives status ACTIVE.
5. User can be connected to an organization.
6. User receives role SUPER_ADMIN, LAB_ADMIN or LIMITED_EMPLOYEE.
7. If organization is provided and does not exist, system returns 404.
8. If email already exists, system returns validation error.

User story:
As a SUPER_ADMIN,
I want to change a user role,
so that I can control user permissions in the system.

Acceptance criteria:
1. SUPER_ADMIN can change user role to SUPER_ADMIN, LAB_ADMIN or LIMITED_EMPLOYEE.
2. Role is required.
3. Role must be SUPER_ADMIN, LAB_ADMIN or LIMITED_EMPLOYEE.
4. If user does not exist, system returns 404.
5. If organization is required and does not exist, system returns 404.

User story:
As a SUPER_ADMIN,
I want to disable a user,
so that the user can no longer access the system.

Acceptance criteria:
1. SUPER_ADMIN can disable a user.
2. User status changes to DISABLED.
3. Disabled user cannot log in.
4. If user does not exist, system returns 404.

### 3. Labs

User story:
As a SUPER_ADMIN,
I want to create a lab,
so that rooms and monitoring equipment can be managed inside it.

Acceptance criteria:
1. SUPER_ADMIN can create lab with name, location and description.
2. Lab name is required.
3. Lab name must not be longer than 150 characters.
4. Lab is connected to organization.
5. Lab is active after creation.
6. After creation, system returns id, organizationId, name, location, description, active, createdAt, updatedAt.
7. If organization does not exist, system returns 404.

User story:
As a LIMITED_EMPLOYEE,
I want to view and search labs,
so that I can find the laboratory I need to monitor.

Acceptance criteria:
1. LIMITED_EMPLOYEE can view list of labs in own organization.
2. LIMITED_EMPLOYEE can search labs by name.
3. LIMITED_EMPLOYEE cannot view labs from another organization.
4. Empty result returns empty list, not error.

User story:
As a SUPER_ADMIN,
I want to deactivate and reactivate a lab,
so that monitoring can be suspended without deleting its structure.

Acceptance criteria:
1. Deactivating a lab preserves the active state of its rooms and sensors.
2. Rooms and sensors in an inactive lab cannot be activated or accept new data.
3. Reactivating the lab restores access for children that are individually active.

### 4. Rooms

User story:
As a SUPER_ADMIN,
I want to create a room,
so that sensors and devices can be connected to a specific place.

Acceptance criteria:
1. SUPER_ADMIN can create room with labId, name, type, floor and area.
2. Room name is required.
3. Room type is required.
4. Room type must be one of allowed room types.
5. Area must be greater than 0 if provided.
6. Room is active after creation.
7. If lab does not exist, system returns 404.
8. A room cannot be created in an inactive lab.

User story:
As a SUPER_ADMIN,
I want to deactivate and reactivate a room,
so that monitoring can be suspended without deleting its sensors.

Acceptance criteria:
1. Deactivating a room preserves the active state of its sensors.
2. Sensors in an inactive room cannot be created, activated, or accept readings.
3. A room can be reactivated only when its lab is active.

User story:
As a LIMITED_EMPLOYEE,
I want to view room details,
so that I can select and monitor a room I am allowed to access.

Acceptance criteria:
1. LIMITED_EMPLOYEE can view room details.
2. Room details include basic room information.
3. Sensors, readings, sessions, and alerts are obtained from their dedicated
   endpoints using the room identifier.
4. LIMITED_EMPLOYEE cannot view rooms outside the granted membership scope.
5. If room does not exist, system returns 404.

### 5. Sensors

User story:
As a SUPER_ADMIN,
I want to add a sensor to a room,
so that the room can be monitored.

Acceptance criteria:
1. SUPER_ADMIN can add sensor with roomId, name, type and unit.
2. Sensor name is required.
3. Sensor type is required.
4. Sensor type must be one of allowed sensor types.
5. Sensor status is OFFLINE by default.
6. Sensor is active after creation.
7. If room does not exist, system returns 404.
8. A sensor cannot be added to an inactive room or lab.

User story:
As a LAB_ADMIN,
I want to update sensor settings,
so that sensor behavior matches laboratory process requirements.

Acceptance criteria:
1. LAB_ADMIN can update sensor name, unit and operational settings.
2. Sensor type and room cannot be changed by this operation.
3. If sensor does not exist, system returns 404.
4. LAB_ADMIN cannot add or deactivate sensors.

User story:
As a LAB_ADMIN,
I want to set a safe value range for a sensor,
so that the system can detect dangerous values.

Acceptance criteria:
1. LAB_ADMIN can set minSafeValue.
2. LAB_ADMIN can set maxSafeValue.
3. minSafeValue must be less than maxSafeValue when both are provided.
4. Safe range is visible in sensor details.
5. If sensor does not exist, system returns 404.

User story:
As a LIMITED_EMPLOYEE,
I want to view current sensor readings,
so that I can monitor laboratory conditions.

Acceptance criteria:
1. LIMITED_EMPLOYEE can view current reading for a sensor.
2. Current reading is the latest reading by measuredAt, with the reading ID as
   the deterministic descending tie-breaker for equal timestamps.
3. Reading includes value, unit and measuredAt.
4. If sensor has no readings, system returns empty current reading.
5. LIMITED_EMPLOYEE cannot view sensors from another organization.

### 6. Cameras — future scope

User story:
As a SUPER_ADMIN,
I want to add a camera to a room,
so that the room can be monitored visually.

Acceptance criteria:
1. SUPER_ADMIN can add camera with roomId, name, model, streamUrl and ipAddress.
2. Camera name is required.
3. Camera status is OFFLINE by default.
4. Camera is active after creation.
5. If room does not exist, system returns 404.

User story:
As a LAB_ADMIN,
I want to update camera operational settings,
so that camera behavior matches laboratory monitoring requirements.

Acceptance criteria:
1. LAB_ADMIN can update camera name, streamUrl, recording and nightVisionOn.
2. LAB_ADMIN cannot add or deactivate cameras.
3. If camera does not exist, system returns 404.

User story:
As a LIMITED_EMPLOYEE,
I want to view camera status and events,
so that I can understand whether the camera is working correctly.

Acceptance criteria:
1. LIMITED_EMPLOYEE can view camera details.
2. Camera details include status, streamUrl, recording, nightVisionOn and lastSeenAt.
3. LIMITED_EMPLOYEE can view camera events.
4. Events are sorted by occurredAt descending.
5. LIMITED_EMPLOYEE cannot view cameras from another organization.
6. If camera does not exist, system returns 404.

### 7. Alerts

User story:
As a LIMITED_EMPLOYEE,
I want to view and filter alerts,
so that I can quickly find active or critical problems.

Acceptance criteria:
1. LIMITED_EMPLOYEE can view alerts in own organization.
2. LIMITED_EMPLOYEE can filter alerts by status.
3. LIMITED_EMPLOYEE can filter alerts by severity.
4. Alert list includes title, type, severity, status, roomId and createdAt.
5. Alerts are sorted by createdAt descending.

User story:
As a LIMITED_EMPLOYEE,
I want to acknowledge an alert,
so that other users know the problem is being handled.

Acceptance criteria:
1. LIMITED_EMPLOYEE can acknowledge active alert.
2. Alert status changes to ACKNOWLEDGED.
3. acknowledgedAt is set.
4. acknowledgedByUserId is set.
5. If alert does not exist, system returns 404.
6. Resolved alert cannot be acknowledged again.

User story:
As a LIMITED_EMPLOYEE,
I want to resolve an alert,
so that the incident is marked as finished.

Acceptance criteria:
1. LIMITED_EMPLOYEE can resolve an acknowledged alert.
2. Alert status changes to RESOLVED.
3. resolvedAt is set.
4. resolvedByUserId is set.
5. If alert does not exist, system returns 404.
6. Resolved alert cannot be resolved again.

User story:
As an authorized user,
I want to resolve or reopen an alert with structured context,
so that workflow decisions remain understandable.

Acceptance criteria:
1. Resolution requires an outcome and may include a comment.
2. A false-alarm resolution requires an explanation.
3. Reopening a resolved alert requires a reason.
4. Acknowledgement, resolution, reopening, and automatic recovery are recorded
   in alert history with the acting user where applicable.
5. The user must have alert-management permission for the alert's room.

### 8. Audit Logs — future scope

User story:
As a LAB_ADMIN,
I want to view lab activity logs,
so that I can track important process changes inside my laboratory.

Acceptance criteria:
1. LAB_ADMIN can view logs only for own organization or assigned labs.
2. Logs can be filtered by user.
3. Logs can be filtered by lab.
4. Logs can be filtered by room.
5. Logs can be filtered by action.
6. Logs are sorted by createdAt descending.

User story:
As a SUPER_ADMIN,
I want to view all audit logs,
so that I can monitor system-wide activity.

Acceptance criteria:
1. SUPER_ADMIN can view audit logs for all organizations.
2. Logs can be filtered by organization.
3. Logs can be filtered by user.
4. Logs can be filtered by action.
5. Logs are sorted by createdAt descending.

### 9. Profile

User story:
As a user,
I want to view my profile,
so that I can see my personal account information.

Acceptance criteria:
1. User can view own profile.
2. Profile includes id, email, firstName, lastName, phone and status.
3. User cannot view another user's profile unless they have SUPER_ADMIN permissions.

User story:
As a user,
I want to update my profile,
so that my personal information stays current.

Acceptance criteria:
1. User can update firstName.
2. User can update lastName.
3. User can update phone.
4. firstName is required.
5. lastName is required.
6. updatedAt changes after successful update.

User story:
As a user,
I want to change my password,
so that my account stays secure.

Acceptance criteria:
1. User must provide current password.
2. User must provide new password.
3. New password must be stored as password hash.
4. If current password is incorrect, system returns validation error.
5. After password change, user can log in with new password.
6. Existing refresh-token families for the user are revoked.

### 10. Monitoring Sessions and Events

User story:
As an authenticated user,
I want to create and run a monitoring session for a room,
so that readings, alerts, and human actions share a meaningful time period.

Acceptance criteria:
1. A session is created in `PLANNED` status for an active room and lab.
2. A planned session can be started.
3. Only one session can be `ACTIVE` in a room at a time.
4. An active session can be completed.
5. A planned or active session can be cancelled.
6. Completed and cancelled sessions cannot be restarted.
7. The authenticated user is stored as the session creator.

User story:
As an authenticated user,
I want to record timestamped events during an active session,
so that later analysis can explain changes in sensor readings and alerts.

Acceptance criteria:
1. An event belongs to one active monitoring session.
2. Category, title, and occurrence time are required.
3. Categories are `OBSERVATION`, `INTERVENTION`, `CONFIGURATION_CHANGE`,
   `MAINTENANCE`, `INCIDENT`, and `OTHER`.
4. Event time cannot precede the session start or be in the future.
5. The authenticated user is stored as the event creator.
6. Session events are returned in chronological order.

User story:
As an authenticated user,
I want to inspect and export a started monitoring session,
so that its readings, alerts, and events can be analysed together.

Acceptance criteria:
1. The timeline combines readings, events, and physically overlapping alerts.
2. Long timelines retain at most 200 readings per sensor, sampled across the
   complete time range with the first and last reading retained.
3. A started session can be exported as a ZIP containing `session.csv`,
   `readings.csv`, `events.csv`, and `alerts.csv`.
4. Reading context covers 15 minutes before and after the session and identifies
   each row as `BEFORE`, `DURING`, or `AFTER`.
5. Exported records include stable entity and contextual session IDs.
6. Alert export distinguishes workflow status from physical condition status.
7. An active session has an empty `ended_at`; current time is only an effective
   query boundary.
8. A cancelled session that never started has no timeline or export.

### 11. Devices and Device Ingestion

User story:
As a SUPER_ADMIN,
I want to register a room-scoped device and configure its sensor channels,
so that a physical or browser data client can submit readings.

Acceptance criteria:
1. A device belongs to one room and has a name, type, and status.
2. A channel can be assigned to an existing sensor in the same room.
3. A sensor and its channel can also be created together.
4. Channel keys are unique within one device.
5. Device administration is restricted to `SUPER_ADMIN`.

User story:
As a SUPER_ADMIN,
I want to manage device credentials,
so that device access can be provisioned and revoked safely.

Acceptance criteria:
1. Provisioning or rotation returns the raw credential only once.
2. Only a BCrypt credential hash is persisted.
3. Rotation revokes the previous active credential.
4. A credential can be revoked explicitly.

User story:
As a device client,
I want to submit an idempotent timestamped reading for a configured channel,
so that retries do not create duplicates.

Acceptance criteria:
1. Requests authenticate with `Authorization: Device <token>`.
2. `channel`, `value`, `measuredAt`, and `messageId` are required.
3. The device, credential, sensor, room, and lab must be operational.
4. Timestamps must satisfy the configured past-age and future-skew policy.
5. Reusing one `messageId` for the same device returns `already_processed` and
   the original reading identifiers.
6. Accepted device readings use the same liveness, threshold, and alert flow as
   user-authenticated readings.
