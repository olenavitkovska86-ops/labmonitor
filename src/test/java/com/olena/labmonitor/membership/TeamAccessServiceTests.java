package com.olena.labmonitor.membership;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamAccessServiceTests {
    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LabRepository labs = mock(LabRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final TeamAccessService service = new TeamAccessService(memberships, users, labs, rooms);
    private Organization organization;
    private User actor;
    private User employee;
    private Membership actorMembership;
    private Membership employeeMembership;

    @BeforeEach
    void setUp() {
        organization = new Organization("Org", null); id(organization, 1L);
        actor = new User("admin@example.com", "hash", "Lab", "Admin", null); id(actor, 10L);
        employee = new User("employee@example.com", "hash", "Limited", "Employee", null); id(employee, 20L);
        actorMembership = new Membership(organization, actor, "LAB_ADMIN"); id(actorMembership, 100L);
        employeeMembership = new Membership(organization, employee, "LIMITED_EMPLOYEE"); id(employeeMembership, 200L);
        employeeMembership.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.SPECIFIC, Set.of(lab(11L)), Set.of());
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(actor));
        when(memberships.findByUserIdAndOrganizationId(10L, 1L)).thenReturn(Optional.of(actorMembership));
        when(memberships.findByUserIdAndOrganizationId(20L, 1L)).thenReturn(Optional.of(employeeMembership));
    }

    @Test
    void rejectsOrganizationWideGrantFromScopedAdmin() {
        Lab allowed = lab(11L);
        actorMembership.updateAccess("LAB_ADMIN", MembershipScopeType.SPECIFIC, Set.of(allowed), Set.of());

        assertThatThrownBy(() -> service.updateScope(1L, 20L,
                new MembershipScopeRequest(MembershipScopeType.ORGANIZATION, Set.of(), Set.of()),
                "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("organization-wide");
    }

    @Test
    void rejectsResourcesOutsideAdminScope() {
        Lab allowed = lab(11L);
        Lab forbidden = lab(12L);
        actorMembership.updateAccess("LAB_ADMIN", MembershipScopeType.SPECIFIC, Set.of(allowed), Set.of());
        when(labs.findAllById(Set.of(12L))).thenReturn(java.util.List.of(forbidden));
        when(rooms.findAllById(Set.of())).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service.updateScope(1L, 20L,
                new MembershipScopeRequest(MembershipScopeType.SPECIFIC, Set.of(12L), Set.of()),
                "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outside your assigned resources");
    }

    @Test
    void rejectsManagingAnotherLabAdmin() {
        employeeMembership.updateAccess("LAB_ADMIN", MembershipScopeType.ORGANIZATION, Set.of(), Set.of());
        assertThatThrownBy(() -> service.updateScope(1L, 20L,
                new MembershipScopeRequest(MembershipScopeType.ORGANIZATION, Set.of(), Set.of()),
                "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("limited employees");
    }

    @Test
    void scopedAdminDoesNotSeeOrganizationWideEmployees() {
        Lab allowed = lab(11L);
        actorMembership.updateAccess("LAB_ADMIN", MembershipScopeType.SPECIFIC, Set.of(allowed), Set.of());
        employeeMembership.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.ORGANIZATION, Set.of(), Set.of());
        when(users.findByOrganizationId(1L)).thenReturn(java.util.List.of(employee));

        assertThat(service.findTeam(1L, "admin@example.com")).isEmpty();
    }

    @Test
    void scopedResponseUsesSharedContractWithoutOutsideAssignments() {
        Lab allowed = lab(11L);
        Lab outside = lab(12L);
        actorMembership.updateAccess("LAB_ADMIN", MembershipScopeType.SPECIFIC, Set.of(allowed), Set.of());
        employeeMembership.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.SPECIFIC, Set.of(allowed, outside), Set.of());
        when(users.findByOrganizationId(1L)).thenReturn(java.util.List.of(employee));

        var response = service.findTeam(1L, "admin@example.com").getFirst();

        assertThat(response.status()).isNull();
        assertThat(response.globalRole()).isNull();
        assertThat(response.allowedActions()).containsExactly("ACCESS_EDIT");
        assertThat(response.assignments()).singleElement().satisfies(assignment -> {
            assertThat(assignment.organizationId()).isEqualTo(1L);
            assertThat(assignment.scopeType()).isEqualTo(MembershipScopeType.SPECIFIC);
            assertThat(assignment.labIds()).containsExactly(11L);
        });
    }

    private Lab lab(Long id) { Lab lab = new Lab(organization, "Lab " + id, null, null); id(lab, id); return lab; }
    private static void id(Object target, Long id) { ReflectionTestUtils.setField(target, "id", id); }
}
