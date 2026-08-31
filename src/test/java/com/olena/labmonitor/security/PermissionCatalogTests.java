package com.olena.labmonitor.security;

import com.olena.labmonitor.membership.MembershipScopeType;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class PermissionCatalogTests {
    @Test void catalogContainsAllCurrentAndPlannedActionNames() {
        assertThat(PermissionCatalog.ALL).contains("profile.read", "profile.update", "password.change",
                "resources.read", "resources.export", "organizations.manage", "labs.manage", "rooms.manage",
                "sensors.manage", "sensors.settings.update", "users.manage", "memberships.manage",
                "team.access.manage", "alerts.manage", "sessions.manage", "system.read");
        assertThat(Permissions.global("SUPER_ADMIN")).containsAll(Set.of("users.manage", "memberships.manage", "team.access.manage"));
        assertThat(Permissions.organization("LAB_ADMIN")).contains("team.access.manage", "sensors.settings.update");
        assertThat(Permissions.organization("LIMITED_EMPLOYEE")).contains("resources.read").doesNotContain("team.access.manage");
    }

    @Test void currentScopeContractIsExplicit() {
        assertThat(MembershipScopeType.values()).containsExactly(MembershipScopeType.ORGANIZATION, MembershipScopeType.SPECIFIC);
        assertThat(Permissions.global("LAB_ADMIN")).doesNotContain("users.manage", "organizations.manage");
        assertThat(Permissions.global("LIMITED_EMPLOYEE")).doesNotContain("users.manage", "memberships.manage");
    }
}
