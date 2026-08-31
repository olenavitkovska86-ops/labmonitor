package com.olena.labmonitor.security;

import java.util.Set;

/** Canonical action names for authorization documentation and future migration. */
public final class PermissionCatalog {
    public static final String PROFILE_READ = "profile.read";
    public static final String PROFILE_UPDATE = "profile.update";
    public static final String PASSWORD_CHANGE = "password.change";
    public static final String RESOURCES_READ = "resources.read";
    public static final String RESOURCES_EXPORT = "resources.export";
    /** Legacy string currently exposed by Permissions.organization(). */
    public static final String LEGACY_EXPORTS_READ = "exports.read";
    public static final String ORGANIZATIONS_MANAGE = "organizations.manage";
    public static final String LABS_MANAGE = "labs.manage";
    public static final String ROOMS_MANAGE = "rooms.manage";
    public static final String SENSORS_MANAGE = "sensors.manage";
    public static final String SENSORS_SETTINGS_UPDATE = "sensors.settings.update";
    public static final String USERS_MANAGE = "users.manage";
    public static final String MEMBERSHIPS_MANAGE = "memberships.manage";
    public static final String TEAM_ACCESS_MANAGE = "team.access.manage";
    public static final String ALERTS_MANAGE = "alerts.manage";
    public static final String SESSIONS_MANAGE = "sessions.manage";
    public static final String SYSTEM_READ = "system.read";

    public static final Set<String> ALL = Set.of(PROFILE_READ, PROFILE_UPDATE, PASSWORD_CHANGE,
            RESOURCES_READ, RESOURCES_EXPORT, LEGACY_EXPORTS_READ, ORGANIZATIONS_MANAGE,
            LABS_MANAGE, ROOMS_MANAGE, SENSORS_MANAGE, SENSORS_SETTINGS_UPDATE, USERS_MANAGE,
            MEMBERSHIPS_MANAGE, TEAM_ACCESS_MANAGE, ALERTS_MANAGE, SESSIONS_MANAGE, SYSTEM_READ);
    private PermissionCatalog() {}
}
