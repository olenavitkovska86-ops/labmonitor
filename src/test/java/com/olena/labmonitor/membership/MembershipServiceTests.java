package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.membership.dto.SaveMembershipRequest;
import com.olena.labmonitor.membership.dto.UpdateMembershipRequest;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipServiceTests {
    private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final LabRepository labRepository = mock(LabRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final MembershipService service = new MembershipService(
            membershipRepository, userRepository, organizationRepository, labRepository, roomRepository);

    private Organization organization;
    private User user;

    @BeforeEach
    void setUp() {
        organization = organization(1L, "Organization");
        user = user(2L, "employee@example.com");
    }

    @Test
    void createsOrganizationWideMembershipWithoutResourceSelections() {
        var request = new SaveMembershipRequest(2L, 1L, "LIMITED_EMPLOYEE",
                new MembershipScopeRequest(MembershipScopeType.ORGANIZATION, Set.of(), Set.of()));
        when(membershipRepository.findByUserIdAndOrganizationId(2L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(membershipRepository.save(org.mockito.ArgumentMatchers.any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertThat(response.role()).isEqualTo("LIMITED_EMPLOYEE");
        assertThat(response.scopeType()).isEqualTo(MembershipScopeType.ORGANIZATION);
        assertThat(response.labIds()).isEmpty();
        assertThat(response.roomIds()).isEmpty();
    }

    @Test
    void storesSpecificLabsAndRemovesRedundantRoomsInsideWholeLab() {
        Lab lab = lab(10L, organization);
        Room room = room(20L, lab);
        Membership membership = membership(30L, organization, user);
        when(membershipRepository.findById(30L)).thenReturn(Optional.of(membership));
        when(labRepository.findAllById(Set.of(10L))).thenReturn(java.util.List.of(lab));
        when(roomRepository.findAllById(Set.of(20L))).thenReturn(java.util.List.of(room));

        var response = service.update(30L, new UpdateMembershipRequest("LAB_ADMIN",
                new MembershipScopeRequest(MembershipScopeType.SPECIFIC, Set.of(10L), Set.of(20L))));

        assertThat(response.role()).isEqualTo("LAB_ADMIN");
        assertThat(response.labIds()).containsExactly(10L);
        assertThat(response.roomIds()).isEmpty();
    }

    @Test
    void rejectsResourcesFromAnotherOrganization() {
        Organization otherOrganization = organization(99L, "Other");
        Lab otherLab = lab(10L, otherOrganization);
        Membership membership = membership(30L, organization, user);
        when(membershipRepository.findById(30L)).thenReturn(Optional.of(membership));
        when(labRepository.findAllById(Set.of(10L))).thenReturn(java.util.List.of(otherLab));

        assertThatThrownBy(() -> service.update(30L, new UpdateMembershipRequest("LAB_ADMIN",
                new MembershipScopeRequest(MembershipScopeType.SPECIFIC, Set.of(10L), Set.of()))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("membership organization");
    }

    @Test
    void rejectsEmptySpecificScope() {
        Membership membership = membership(30L, organization, user);
        when(membershipRepository.findById(30L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.update(30L, new UpdateMembershipRequest("LAB_ADMIN",
                new MembershipScopeRequest(MembershipScopeType.SPECIFIC, Set.of(), Set.of()))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void deletesExistingMembership() {
        Membership membership = membership(30L, organization, user);
        when(membershipRepository.findById(30L)).thenReturn(Optional.of(membership));

        service.delete(30L);

        verify(membershipRepository).delete(membership);
    }

    private static Organization organization(Long id, String name) {
        Organization organization = new Organization(name, null);
        ReflectionTestUtils.setField(organization, "id", id);
        return organization;
    }

    private static User user(Long id, String email) {
        User user = new User(email, "hash", "Test", "User", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Lab lab(Long id, Organization organization) {
        Lab lab = new Lab(organization, "Lab", null, null);
        ReflectionTestUtils.setField(lab, "id", id);
        return lab;
    }

    private static Room room(Long id, Lab lab) {
        Room room = new Room(lab, "Room", RoomType.SERVER_ROOM, null, null);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private static Membership membership(Long id, Organization organization, User user) {
        Membership membership = new Membership(organization, user, "LIMITED_EMPLOYEE");
        ReflectionTestUtils.setField(membership, "id", id);
        return membership;
    }
}
