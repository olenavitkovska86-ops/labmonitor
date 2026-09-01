## Roles and Permissions

This document describes the effective permission model. The backend combines
Spring method security with hierarchy-aware `AccessPolicy` checks. Camera and
audit-log permissions are future scope and are listed separately at the end.

1. LIMITED_EMPLOYEE

Can:

- view dashboard
- view labs
- view rooms
- view room details
- view sensors
- view sensor details
- view current sensor readings
- view sensor readings history
- view alerts
- filter alerts
- acknowledge alerts
- resolve alerts
- reopen resolved alerts
- create and manage monitoring sessions and session events in granted rooms
- export permitted readings and monitoring sessions
- view own profile
- update own profile
- change own password

Cannot:

- create/update/delete organizations
- create/update/activate/deactivate labs
- create/update/activate/deactivate rooms
- add/update/activate/deactivate sensors
- set sensor safe range
- create/invite users
- disable users
- change user roles
- view audit logs


2. LAB_ADMIN

Can do everything LIMITED_EMPLOYEE can, plus:

Sensors:

- update sensor settings
- set sensor safe range

Team access:

- view limited employees whose access intersects the admin's own scope
- assign those employees to labs and rooms inside the admin's own scope

Cannot:

- create/update/delete organizations
- create/update/activate/deactivate labs
- create/update/activate/deactivate rooms
- add/activate/deactivate sensors
- create/invite users
- disable users
- change user roles
- view system-wide audit logs


3. SUPER_ADMIN

Can do everything LAB_ADMIN can, plus:

Organizations:

- view all organizations
- search organizations
- create organization
- update any organization
- delete organization

Users:

- view all users
- search all users
- create users
- disable users
- assign roles
- change user roles
- remove users from organizations

Labs:

- create lab
- update lab
- activate lab
- deactivate lab

Rooms:

- create room
- update room
- activate room
- deactivate room

Sensors:

- add sensor
- update sensor
- set sensor safe range
- activate sensor
- deactivate sensor

Devices:

- register and update devices
- assign or create sensor channels
- provision, rotate, and revoke device credentials


Permission hierarchy:

LIMITED_EMPLOYEE
    ↓
LAB_ADMIN = LIMITED_EMPLOYEE + lab process/equipment settings
    ↓
SUPER_ADMIN = LAB_ADMIN + system structure and user management


## Scoped LAB_ADMIN responsibility model

> **Status: IMPLEMENTED.** Structural administration remains a `SUPER_ADMIN`
> responsibility.

### Role purpose

`LAB_ADMIN` is an organization-scoped operational administrator. The role is
responsible for laboratory processes and equipment only inside the
organization, labs, or rooms granted by its membership scope. It is not a
system administrator and must not receive global user or organization
management rights.

### Current effective permissions

The current application allows a `LAB_ADMIN` to:

- view granted organizations, labs, rooms, sensors, readings, and analytics — `IMPLEMENTED`
- acknowledge, resolve, and reopen alerts in granted rooms — `IMPLEMENTED`
- create and manage monitoring sessions and session events in granted rooms — `IMPLEMENTED`
- export permitted monitoring data — `IMPLEMENTED`
- update sensor settings and safe ranges in granted rooms — `IMPLEMENTED`
- view and update their own profile and change their password — `IMPLEMENTED`

The current application does not allow a `LAB_ADMIN` to:

- create, activate, deactivate, or delete organizations, labs, or rooms
- create, activate, or deactivate sensors
- manage global users, global roles, or organization memberships
- access system-wide settings or data belonging to another membership scope

### Team access

Scoped admins can view limited employees whose access intersects their own and
assign those existing members to labs or rooms within that scope. They cannot
create users, change roles, edit lab or room metadata, or expand access beyond
their own scope.

Creating or deleting structural resources, inviting or disabling users,
changing global roles, and managing other organizations remain
`SUPER_ADMIN` responsibilities.

### Navigation

The application shell exposes role-aware administration navigation:

- `Users & access` provides global administration to `SUPER_ADMIN` and scoped
  team assignments to `LAB_ADMIN`.
- `Sensor settings` is available to `SUPER_ADMIN` and `LAB_ADMIN`.

The contextual `Sensor settings` action in Monitor should remain available;
the administration entry is an additional discovery path, not a replacement.

## Future permissions

Camera management, camera viewing, audit logs, and configurable per-sensor
alert rules are not implemented in the current MVP. Their detailed permission
model should be documented when those features are introduced.
