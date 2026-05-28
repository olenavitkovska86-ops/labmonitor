 ## Roles and Permissions

  1. Operator

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
  - invite users
  - change user roles
  - create/update/deactivate labs
  - create/update/deactivate rooms
  - add/update/deactivate sensors
  - set sensor safe range
  - add/update/deactivate cameras
  - view audit logs


  2. Organization Admin

  Can do everything Operator can, plus:

  Organization:
  - view organization info
  - update organization info

  Users:
  - view organization users
  - search organization users
  - invite user by email
  - change user role
  - disable user access

  Labs:
  - create lab
  - update lab
  - deactivate lab

  Rooms:
  - create room
  - update room
  - deactivate room

  Sensors:
  - add sensor
  - update sensor
  - set safe value range
  - deactivate sensor

  Cameras:
  - add camera
  - update camera
  - deactivate camera

  Audit:
  - view organization audit logs
  - filter organization audit logs


  3. System Admin

  Can do everything Organization Admin can, plus:

  Organizations:
  - view all organizations
  - search organizations
  - create organization
  - update any organization
  - delete organization

  System users:
  - view all users
  - search all users
  - disable any user

  Audit:
  - view all audit logs
  - filter all audit logs by organization
  - filter all audit logs by user
  - filter all audit logs by action


  Permission hierarchy:

  Operator
      ↓
  Organization Admin = Operator + organization management
      ↓
  System Admin = Organization Admin + system management
