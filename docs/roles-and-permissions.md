## Roles and Permissions

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
