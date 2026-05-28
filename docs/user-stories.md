## User Stories

### 1. Organizations

User story:
As a System Admin,
I want to create an organization,
so that I can register a laboratory center in the system.

Acceptance criteria:
1. System Admin can create an organization with name and description.
2. Organization name is required.
3. Organization name must not be longer than 150 characters.
4. Organization description must not be longer than 500 characters.
5. After creation, system returns id, name, description, createdAt, updatedAt.
6. System Admin can get organization by id.
7. System Admin can view list of organizations.
8. If organization by id does not exist, system returns 404.

User story:
As a System Admin,
I want to update organization information,
so that organization data stays correct and up to date.

Acceptance criteria:
1. System Admin can update organization name.
2. System Admin can update organization description.
3. Organization name is required.
4. Organization name must not be longer than 150 characters.
5. Organization description must not be longer than 500 characters.
6. updatedAt changes after successful update.
7. If organization does not exist, system returns 404.

User story:
As a System Admin,
I want to delete an organization,
so that inactive or incorrect organizations can be removed from the system.

Acceptance criteria:
1. System Admin can delete organization by id.
2. After deletion, organization is no longer available by id.
3. If organization does not exist, system returns 404.
4. Related labs, rooms, sensors, cameras and memberships are removed according to database rules.

### 2. Users and Roles

User story:
As an Organization Admin,
I want to invite a user by email,
so that the user can access my organization.

Acceptance criteria:
1. Organization Admin can invite user by email.
2. Email is required.
3. Email must be unique.
4. Invited user receives status INVITED.
5. Invited user is connected to the organization.
6. Invited user receives role ADMIN or OPERATOR.
7. If organization does not exist, system returns 404.
8. If email already exists, system returns validation error.

User story:
As an Organization Admin,
I want to change a user role,
so that I can control user permissions in the organization.

Acceptance criteria:
1. Organization Admin can change user role to ADMIN or OPERATOR.
2. Role is required.
3. Role must be ADMIN or OPERATOR.
4. User must belong to the organization.
5. If user does not exist, system returns 404.
6. If organization does not exist, system returns 404.

### 3. Labs

User story:
As an Organization Admin,
I want to create a lab,
so that rooms, sensors and cameras can be managed inside it.

Acceptance criteria:
1. Organization Admin can create lab with name, location and description.
2. Lab name is required.
3. Lab name must not be longer than 150 characters.
4. Lab is connected to organization.
5. Lab is active after creation.
6. After creation, system returns id, organizationId, name, location, description, active, createdAt, updatedAt.
7. If organization does not exist, system returns 404.

User story:
As an Operator,
I want to view and search labs,
so that I can find the laboratory I need to monitor.

Acceptance criteria:
1. Operator can view list of labs in own organization.
2. Operator can search labs by name.
3. Operator cannot view labs from another organization.
4. Empty result returns empty list, not error.

### 4. Rooms

User story:
As an Organization Admin,
I want to create a room,
so that sensors and cameras can be connected to a specific place.

Acceptance criteria:
1. Organization Admin can create room with labId, name, type, floor and area.
2. Room name is required.
3. Room type is required.
4. Room type must be one of allowed room types.
5. Area must be greater than 0 if provided.
6. Room is active after creation.
7. If lab does not exist, system returns 404.

User story:
As an Operator,
I want to view room details,
so that I can monitor sensors, cameras, security state and alerts in the room.

Acceptance criteria:
1. Operator can view room details.
2. Room details include basic room information.
3. Room details include sensors in the room.
4. Room details include cameras in the room.
5. Room details include active alerts.
6. Operator cannot view rooms from another organization.
7. If room does not exist, system returns 404.

### 5. Sensors

User story:
As an Organization Admin,
I want to add a sensor to a room,
so that the room can be monitored.

Acceptance criteria:
1. Organization Admin can add sensor with roomId, name, type and unit.
2. Sensor name is required.
3. Sensor type is required.
4. Sensor type must be one of allowed sensor types.
5. Sensor status is OFFLINE by default.
6. Sensor is active after creation.
7. If room does not exist, system returns 404.

User story:
As an Organization Admin,
I want to set a safe value range for a sensor,
so that the system can detect dangerous values.

Acceptance criteria:
1. Organization Admin can set minSafeValue.
2. Organization Admin can set maxSafeValue.
3. minSafeValue must be less than maxSafeValue when both are provided.
4. Safe range is visible in sensor details.
5. If sensor does not exist, system returns 404.

User story:
As an Operator,
I want to view current sensor readings,
so that I can monitor laboratory conditions.

Acceptance criteria:
1. Operator can view current reading for a sensor.
2. Current reading is the latest reading by measuredAt.
3. Reading includes value, unit and measuredAt.
4. If sensor has no readings, system returns empty current reading.
5. Operator cannot view sensors from another organization.

### 6. Cameras

User story:
As an Organization Admin,
I want to add a camera to a room,
so that the room can be monitored visually.

Acceptance criteria:
1. Organization Admin can add camera with roomId, name, model, streamUrl and ipAddress.
2. Camera name is required.
3. Camera status is OFFLINE by default.
4. Camera is active after creation.
5. If room does not exist, system returns 404.

User story:
As an Operator,
I want to view camera status and events,
so that I can understand whether the camera is working correctly.

Acceptance criteria:
1. Operator can view camera details.
2. Camera details include status, streamUrl, recording, nightVisionOn and lastSeenAt.
3. Operator can view camera events.
4. Events are sorted by occurredAt descending.
5. Operator cannot view cameras from another organization.
6. If camera does not exist, system returns 404.

### 7. Alerts

User story:
As an Operator,
I want to view and filter alerts,
so that I can quickly find active or critical problems.

Acceptance criteria:
1. Operator can view alerts in own organization.
2. Operator can filter alerts by status.
3. Operator can filter alerts by severity.
4. Alert list includes title, type, severity, status, roomId and createdAt.
5. Alerts are sorted by createdAt descending.

User story:
As an Operator,
I want to acknowledge an alert,
so that other users know the problem is being handled.

Acceptance criteria:
1. Operator can acknowledge active alert.
2. Alert status changes to ACKNOWLEDGED.
3. acknowledgedAt is set.
4. acknowledgedByUserId is set.
5. If alert does not exist, system returns 404.
6. Resolved alert cannot be acknowledged again.

User story:
As an Operator,
I want to resolve an alert,
so that the incident is marked as finished.

Acceptance criteria:
1. Operator can resolve active or acknowledged alert.
2. Alert status changes to RESOLVED.
3. resolvedAt is set.
4. resolvedByUserId is set.
5. If alert does not exist, system returns 404.
6. Resolved alert cannot be resolved again.

### 8. Audit Logs

User story:
As an Organization Admin,
I want to view organization audit logs,
so that I can track changes inside my organization.

Acceptance criteria:
1. Organization Admin can view audit logs only for own organization.
2. Logs can be filtered by user.
3. Logs can be filtered by lab.
4. Logs can be filtered by room.
5. Logs can be filtered by action.
6. Logs are sorted by createdAt descending.

User story:
As a System Admin,
I want to view all audit logs,
so that I can monitor system-wide activity.

Acceptance criteria:
1. System Admin can view audit logs for all organizations.
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
3. User cannot view another user's profile unless they have admin permissions.

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
