package com.olena.labmonitor.security;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipScopeType;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessPolicyTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccessPolicy policy = new AccessPolicy(userRepository);

    @Test
    void superAdminCanAccessEveryResourceWithoutMemberships() {
        User user = user("admin@example.com");
        user.setGlobalRole("SUPER_ADMIN");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var access = policy.forAuthentication(authentication(user));

        assertThat(access.canViewOrganization(999L)).isTrue();
        assertThat(access.canViewRoom(999L, 888L, 777L)).isTrue();
        assertThat(access.canManageSensor(999L, 888L, 777L)).isTrue();
    }

    @Test
    void organizationWideMembershipDoesNotGrantAnotherOrganization() {
        User user = user("employee@example.com");
        Organization organization = organization(1L);
        user.getMemberships().add(new Membership(organization, user, "LIMITED_EMPLOYEE"));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var access = policy.forAuthentication(authentication(user));

        assertThat(access.canViewRoom(1L, 10L, 20L)).isTrue();
        assertThat(access.canViewOrganization(2L)).isFalse();
        assertThat(access.canViewRoom(2L, 10L, 20L)).isFalse();
        assertThat(access.canManageSensor(1L, 10L, 20L)).isFalse();
    }

    @Test
    void roomOnlyScopeExposesParentLabButNotSiblingRoom() {
        User user = user("employee@example.com");
        Organization organization = organization(1L);
        Lab lab = lab(10L, organization);
        Room allowedRoom = room(20L, lab);
        Membership membership = new Membership(organization, user, "LIMITED_EMPLOYEE");
        membership.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.SPECIFIC, Set.of(), Set.of(allowedRoom));
        user.getMemberships().add(membership);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var access = policy.forAuthentication(authentication(user));

        assertThat(access.canViewLab(1L, 10L)).isTrue();
        assertThat(access.canViewRoom(1L, 10L, 20L)).isTrue();
        assertThat(access.canViewRoom(1L, 10L, 21L)).isFalse();
    }

    @Test
    void labAdminCanManageSensorsOnlyInsideAssignedLab() {
        User user = user("lab-admin@example.com");
        Organization organization = organization(1L);
        Lab lab = lab(10L, organization);
        Membership membership = new Membership(organization, user, "LAB_ADMIN");
        membership.updateAccess("LAB_ADMIN", MembershipScopeType.SPECIFIC, Set.of(lab), Set.of());
        user.getMemberships().add(membership);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var access = policy.forAuthentication(authentication(user));

        assertThat(access.canManageSensor(1L, 10L, 20L)).isTrue();
        assertThat(access.canManageSensor(1L, 11L, 21L)).isFalse();
    }

    private static UsernamePasswordAuthenticationToken authentication(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null, java.util.List.of());
    }

    private static User user(String email) {
        return new User(email, "hash", "Test", "User", null);
    }

    private static Organization organization(Long id) {
        Organization organization = new Organization("Organization " + id, null);
        ReflectionTestUtils.setField(organization, "id", id);
        return organization;
    }

    private static Lab lab(Long id, Organization organization) {
        Lab lab = new Lab(organization, "Lab " + id, null, null);
        ReflectionTestUtils.setField(lab, "id", id);
        return lab;
    }

    private static Room room(Long id, Lab lab) {
        Room room = new Room(lab, "Room " + id, RoomType.SERVER_ROOM, null, null);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }
}
