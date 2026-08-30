package com.olena.labmonitor.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Permissions {
    private static final List<String> PROFILE = List.of(
            "profile.read", "profile.update", "password.change");
    private static final List<String> EMPLOYEE = List.of(
            "resources.read", "alerts.manage", "sessions.manage", "exports.read");
    private static final List<String> LAB_ADMIN = List.of(
            "sensors.settings.update", "team.access.manage");
    private static final List<String> SUPER_ADMIN = List.of(
            "organizations.manage", "labs.manage", "rooms.manage", "sensors.manage",
            "users.manage", "memberships.manage", "system.read");

    private Permissions() {
    }

    public static List<String> global(String globalRole) {
        Set<String> result = new LinkedHashSet<>(PROFILE);
        if ("SUPER_ADMIN".equals(globalRole)) {
            result.addAll(EMPLOYEE);
            result.addAll(LAB_ADMIN);
            result.addAll(SUPER_ADMIN);
        }
        return List.copyOf(result);
    }

    public static List<String> organization(String role) {
        Set<String> result = new LinkedHashSet<>(EMPLOYEE);
        if ("LAB_ADMIN".equals(role)) result.addAll(LAB_ADMIN);
        return List.copyOf(result);
    }
}
