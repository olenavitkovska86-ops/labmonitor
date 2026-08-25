# Early Menu Sketch

This file is an early product-planning artifact, not a description of the
current web interface. Camera and audit-log items are outside the current MVP.
The implemented browser navigation is documented in `README.md`; monitoring
sessions are the current feature under development.

## Menus:

0. Main Menu
    1. Log in
    0. Exit

1. Log in
    Enter your email:
    Enter your password:
    Welcome, $user!
    --> (2. Limited Employee Menu) / (3. Lab Admin Menu) / (4. Super Admin Menu)

2. Limited Employee Menu
    1. Dashboard
        1. View lab monitoring overview
        2. View room monitoring overview
        3. View active alerts summary
        4. View sensor status summary
        5. View camera status summary
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    2. Monitor Labs
        1. View and search labs
        2. View lab details
            1. View rooms in lab
            2. View sensors in lab
            3. View cameras in lab
            4. View active alerts
            0. Back to Monitor Labs --> (2. Monitor Labs)
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    3. Monitor Rooms
        1. View and search rooms
        2. View room details
            1. View sensors in room
            2. View cameras in room
            3. View security state
            4. View energy readings
            5. View active alerts
            0. Back to Monitor Rooms --> (3. Monitor Rooms)
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    4. Monitor Sensors
        1. View and search sensors
        2. View sensor details
            1. View current reading
            2. View readings history
            3. View safe value range
            0. Back to Monitor Sensors --> (4. Monitor Sensors)
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    5. Monitor Cameras
        1. View and search cameras
        2. View camera details
            1. View camera status
            2. View stream URL
            3. View camera events
            0. Back to Monitor Cameras --> (5. Monitor Cameras)
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    6. Manage Alerts
        1. View and filter alerts
        2. View alert details
            1. Acknowledge alert
            2. Resolve alert
            0. Back to Manage Alerts --> (6. Manage Alerts)
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    7. My Profile
        1. View my profile
        2. Update profile info
        3. Change password
        0. Back to Limited Employee Menu --> (2. Limited Employee Menu)

    0. Log Out --> (0. Main Menu)

3. Lab Admin Menu
    Includes all Limited Employee Menu options, plus:

    8. Manage Sensor Settings
        1. Update sensor settings
        2. Set safe value range
        0. Back to Lab Admin Menu --> (3. Lab Admin Menu)

    9. Manage Camera Settings
        1. Update camera operational settings
        0. Back to Lab Admin Menu --> (3. Lab Admin Menu)

    10. Manage Alert Notes
        1. Add alert note
        2. Add resolution note
        0. Back to Lab Admin Menu --> (3. Lab Admin Menu)

    11. Lab Activity Logs
        1. View lab activity logs
        2. View process audit logs
        0. Back to Lab Admin Menu --> (3. Lab Admin Menu)

    0. Log Out --> (0. Main Menu)

4. Super Admin Menu
    Includes all Lab Admin Menu options, plus:

    12. Manage Organizations
        1. View and search organizations
        2. Create organization
        3. Update organization
        4. Delete organization
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    13. Manage Users
        1. View and search all users
        2. Create/invite user
        3. Assign role
        4. Change user role
        5. Disable user
        6. Remove user from organization
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    14. Manage Labs
        1. Create lab
        2. Update lab
        3. Activate/deactivate lab
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    15. Manage Rooms
        1. Create room
        2. Update room
        3. Activate/deactivate room
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    16. Manage Sensors
        1. Add sensor
        2. Update sensor
        3. Set safe value range
        4. Activate/deactivate sensor
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    17. Manage Cameras
        1. Add camera
        2. Update camera
        3. Deactivate camera
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    18. System Audit Logs
        1. View and filter all audit logs
        0. Back to Super Admin Menu --> (4. Super Admin Menu)

    0. Log Out --> (0. Main Menu)
