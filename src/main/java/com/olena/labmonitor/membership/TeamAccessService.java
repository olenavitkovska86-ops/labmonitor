package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.membership.dto.AccessAssignmentResponse;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.user.UserRepository;
import com.olena.labmonitor.user.dto.ManagedUserResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TeamAccessService {
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final LabRepository labs;
    private final RoomRepository rooms;

    public TeamAccessService(MembershipRepository memberships, UserRepository users,
                             LabRepository labs, RoomRepository rooms) {
        this.memberships = memberships;
        this.users = users;
        this.labs = labs;
        this.rooms = rooms;
    }

    @Transactional(readOnly = true)
    public List<ManagedUserResponse> findTeam(Long organizationId, String actorEmail) {
        Membership actor = requireLabAdmin(organizationId, actorEmail);
        return users.findByOrganizationId(organizationId).stream()
                .map(user -> memberships.findByUserIdAndOrganizationId(user.getId(), organizationId).orElseThrow())
                .filter(target -> "LIMITED_EMPLOYEE".equals(target.getRole()))
                .filter(target -> intersects(actor, target))
                .map(target -> scopedResponse(actor, target))
                .toList();
    }

    public ManagedUserResponse updateScope(Long organizationId, Long userId,
                                          MembershipScopeRequest scope, String actorEmail) {
        Membership actor = requireLabAdmin(organizationId, actorEmail);
        Membership target = memberships.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization member not found"));
        if (!"LIMITED_EMPLOYEE".equals(target.getRole())) {
            throw new AccessDeniedException("LAB_ADMIN can manage only limited employees");
        }
        if (!intersects(actor, target)) {
            throw new AccessDeniedException("Employee access does not overlap your assigned resources");
        }
        Selection selection = resolveSelection(organizationId, scope);
        requireWithinActorScope(actor, scope.type(), selection);
        if (actor.getScopeType() == MembershipScopeType.ORGANIZATION) {
            target.updateAccess("LIMITED_EMPLOYEE", scope.type(), selection.labs(), selection.rooms());
        } else {
            Set<Lab> mergedLabs = new LinkedHashSet<>(selection.labs());
            target.getAccessibleLabs().stream().filter(lab -> !includesLab(actor, lab.getId())).forEach(mergedLabs::add);
            Set<Room> mergedRooms = new LinkedHashSet<>(selection.rooms());
            target.getAccessibleRooms().stream().filter(room -> !includesRoom(actor, room)).forEach(mergedRooms::add);
            target.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.SPECIFIC, mergedLabs, mergedRooms);
        }
        return scopedResponse(actor, target);
    }

    private Membership requireLabAdmin(Long organizationId, String email) {
        var actor = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return memberships.findByUserIdAndOrganizationId(actor.getId(), organizationId)
                .filter(item -> "LAB_ADMIN".equals(item.getRole()))
                .orElseThrow(() -> new AccessDeniedException("LAB_ADMIN access is required in this organization"));
    }

    private boolean intersects(Membership actor, Membership target) {
        if (actor.getScopeType() == MembershipScopeType.ORGANIZATION) return true;
        if (target.getScopeType() == MembershipScopeType.ORGANIZATION) return false;
        return target.getAccessibleLabs().stream().anyMatch(lab -> includesLab(actor, lab.getId()))
                || target.getAccessibleRooms().stream().anyMatch(room -> includesRoom(actor, room));
    }

    private ManagedUserResponse scopedResponse(Membership actor, Membership target) {
        if (actor.getScopeType() == MembershipScopeType.ORGANIZATION) {
            return ManagedUserResponse.forLabAdmin(target.getUser(), AccessAssignmentResponse.from(target));
        }
        Set<Long> labIds = target.getAccessibleLabs().stream().filter(lab -> includesLab(actor, lab.getId()))
                .map(Lab::getId).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<Long> roomIds = target.getAccessibleRooms().stream().filter(room -> includesRoom(actor, room))
                .map(Room::getId).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return ManagedUserResponse.forLabAdmin(target.getUser(), AccessAssignmentResponse.from(target, labIds, roomIds));
    }

    private void requireWithinActorScope(Membership actor, MembershipScopeType type, Selection selection) {
        if (type == MembershipScopeType.ORGANIZATION) {
            if (actor.getScopeType() != MembershipScopeType.ORGANIZATION) {
                throw new AccessDeniedException("Cannot grant organization-wide access outside your scope");
            }
            return;
        }
        if (selection.labs().stream().anyMatch(lab -> !includesLab(actor, lab.getId()))
                || selection.rooms().stream().anyMatch(room -> !includesRoom(actor, room))) {
            throw new AccessDeniedException("Cannot grant access outside your assigned resources");
        }
    }

    private boolean includesLab(Membership membership, Long labId) {
        return membership.getScopeType() == MembershipScopeType.ORGANIZATION
                || membership.getAccessibleLabs().stream().anyMatch(lab -> lab.getId().equals(labId));
    }

    private boolean includesRoom(Membership membership, Room room) {
        return membership.getScopeType() == MembershipScopeType.ORGANIZATION
                || includesLab(membership, room.getLab().getId())
                || membership.getAccessibleRooms().stream().anyMatch(item -> item.getId().equals(room.getId()));
    }

    private Selection resolveSelection(Long organizationId, MembershipScopeRequest scope) {
        Set<Long> labIds = scope.normalizedLabIds();
        Set<Long> roomIds = scope.normalizedRoomIds();
        if (scope.type() == MembershipScopeType.ORGANIZATION) {
            if (!labIds.isEmpty() || !roomIds.isEmpty()) throw new InvalidOperationException("Organization scope cannot contain resources");
            return new Selection(Set.of(), Set.of());
        }
        if (labIds.isEmpty() && roomIds.isEmpty()) throw new InvalidOperationException("Select at least one lab or room");
        List<Lab> selectedLabs = labs.findAllById(labIds);
        List<Room> selectedRooms = rooms.findAllById(roomIds);
        if (selectedLabs.size() != labIds.size() || selectedRooms.size() != roomIds.size()) {
            throw new ResourceNotFoundException("One or more resources were not found");
        }
        if (selectedLabs.stream().anyMatch(lab -> !organizationId.equals(lab.getOrganization().getId()))
                || selectedRooms.stream().anyMatch(room -> !organizationId.equals(room.getLab().getOrganization().getId()))) {
            throw new InvalidOperationException("Resources must belong to the selected organization");
        }
        Set<Lab> labSet = new LinkedHashSet<>(selectedLabs);
        Set<Room> roomSet = new LinkedHashSet<>();
        selectedRooms.stream().filter(room -> !labIds.contains(room.getLab().getId())).forEach(roomSet::add);
        return new Selection(labSet, roomSet);
    }

    private record Selection(Set<Lab> labs, Set<Room> rooms) {}
}
