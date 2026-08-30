## Roles and Permissions

This document describes the target permission model. The backend currently
requires authentication for API access, but detailed method-level role checks
are not yet applied consistently to every organization, lab, room, sensor, and
alert operation. Camera and audit-log permissions below are planned and are not
part of the current MVP.

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
- view cameras
- view camera details
- view camera status
- view camera events
- view alerts
- filter alerts
- acknowledge alerts
- resolve alerts
- view own profile
- update own profile
- change own password

Cannot:
- create/update/delete organizations
- create/update/activate/deactivate labs
- create/update/activate/deactivate rooms
- add/update/activate/deactivate sensors
- set sensor safe range
- add/update/deactivate cameras
- create/invite users
- disable users
- change user roles
- view audit logs


2. LAB_ADMIN

Can do everything LIMITED_EMPLOYEE can, plus:

Sensors:
- update sensor settings
- set sensor safe range

Cameras:
- update camera operational settings

Alerts:
- add alert notes
- add resolution notes

Logs:
- view lab activity logs
- view process audit logs

Cannot:
- create/update/delete organizations
- create/update/activate/deactivate labs
- create/update/activate/deactivate rooms
- add/activate/deactivate sensors
- add/deactivate cameras
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
- create/invite users
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

Cameras:
- add camera
- update camera
- deactivate camera

Audit:
- view all audit logs
- filter all audit logs by organization
- filter all audit logs by user
- filter all audit logs by action


Permission hierarchy:

LIMITED_EMPLOYEE
    ↓
LAB_ADMIN = LIMITED_EMPLOYEE + lab process/equipment settings
    ↓
SUPER_ADMIN = LAB_ADMIN + system structure and user management


## NEW — proposed LAB_ADMIN responsibility model (2026-08-29)

> **Status: PARTIALLY IMPLEMENTED.** Scoped team access is available to
> organization `LAB_ADMIN` memberships. Structural and metadata configuration
> remains a `SUPER_ADMIN` responsibility.

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

### Proposed additions

The following capabilities are considered appropriate for this role but are
not yet implemented:

- editing lab and room metadata remains restricted to `SUPER_ADMIN`
- view the members of their own organization without access to global user
  administration; scoped admins see only limited employees whose access
  intersects their own — `IMPLEMENTED`
- assign existing organization members to labs or rooms inside the admin's
  scope, without creating users or changing roles — `IMPLEMENTED`

Creating or deleting structural resources, inviting or disabling users,
changing global roles, and managing other organizations remain
`SUPER_ADMIN` responsibilities.

### Proposed navigation

The application shell should expose the following role-aware administration
navigation:

- `Users & access` — visible only to `SUPER_ADMIN`
- `Sensor settings` — visible to `SUPER_ADMIN` and `LAB_ADMIN`
- `Users & access` — shared role-aware page: global administration for
  `SUPER_ADMIN`, scoped team assignments for `LAB_ADMIN`

The contextual `Sensor settings` action in Monitor should remain available;
the administration entry is an additional discovery path, not a replacement.
