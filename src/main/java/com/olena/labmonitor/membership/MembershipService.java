package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.membership.dto.MembershipResponse;
import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.membership.dto.SaveMembershipRequest;
import com.olena.labmonitor.membership.dto.UpdateMembershipRequest;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class MembershipService {
    private static final Set<String> ORGANIZATION_ROLES = Set.of("LAB_ADMIN", "LIMITED_EMPLOYEE");

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final LabRepository labRepository;
    private final RoomRepository roomRepository;

    public MembershipService(MembershipRepository membershipRepository, UserRepository userRepository,
                             OrganizationRepository organizationRepository, LabRepository labRepository,
                             RoomRepository roomRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.labRepository = labRepository;
        this.roomRepository = roomRepository;
    }

    public MembershipResponse create(SaveMembershipRequest request) {
        validateRole(request.role());
        if (membershipRepository.findByUserIdAndOrganizationId(request.userId(), request.organizationId()).isPresent()) {
            throw new InvalidOperationException("User already has a membership in this organization");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if ("SUPER_ADMIN".equals(user.getGlobalRole())) {
            throw new InvalidOperationException("SUPER_ADMIN users cannot have organization memberships");
        }
        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        AccessSelection access = resolveAccess(organization.getId(), request.scope());

        Membership membership = new Membership(organization, user, request.role());
        membership.updateAccess(request.role(), request.scope().type(), access.labs(), access.rooms());
        Membership saved = membershipRepository.save(membership);
        user.getMemberships().add(saved);
        return MembershipResponse.from(saved);
    }

    public MembershipResponse update(Long membershipId, UpdateMembershipRequest request) {
        validateRole(request.role());
        Membership membership = getMembership(membershipId);
        AccessSelection access = resolveAccess(membership.getOrganization().getId(), request.scope());
        membership.updateAccess(request.role(), request.scope().type(), access.labs(), access.rooms());
        return MembershipResponse.from(membership);
    }

    public MembershipResponse changeRole(Long userId, Long organizationId, String role) {
        validateRole(role);
        Membership membership = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        membership.updateAccess(role, membership.getScopeType(),
                membership.getAccessibleLabs(), membership.getAccessibleRooms());
        return MembershipResponse.from(membership);
    }

    @Transactional(readOnly = true)
    public MembershipResponse findById(Long membershipId) {
        return MembershipResponse.from(getMembership(membershipId));
    }

    public void delete(Long membershipId) {
        membershipRepository.delete(getMembership(membershipId));
    }

    private Membership getMembership(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
    }

    private void validateRole(String role) {
        if (!ORGANIZATION_ROLES.contains(role)) {
            throw new InvalidOperationException("Invalid organization role: " + role);
        }
    }

    private AccessSelection resolveAccess(Long organizationId, MembershipScopeRequest scope) {
        Set<Long> labIds = scope.normalizedLabIds();
        Set<Long> roomIds = scope.normalizedRoomIds();
        if (scope.type() == MembershipScopeType.ORGANIZATION) {
            if (!labIds.isEmpty() || !roomIds.isEmpty()) {
                throw new InvalidOperationException("Organization-wide scope cannot contain lab or room selections");
            }
            return new AccessSelection(Set.of(), Set.of());
        }
        if (labIds.isEmpty() && roomIds.isEmpty()) {
            throw new InvalidOperationException("Specific scope requires at least one lab or room");
        }

        List<Lab> labs = labRepository.findAllById(labIds);
        if (labs.size() != labIds.size()) {
            throw new ResourceNotFoundException("One or more labs were not found");
        }
        if (labs.stream().anyMatch(lab -> !organizationId.equals(lab.getOrganization().getId()))) {
            throw new InvalidOperationException("All selected labs must belong to the membership organization");
        }

        List<Room> rooms = roomRepository.findAllById(roomIds);
        if (rooms.size() != roomIds.size()) {
            throw new ResourceNotFoundException("One or more rooms were not found");
        }
        if (rooms.stream().anyMatch(room -> !organizationId.equals(room.getLab().getOrganization().getId()))) {
            throw new InvalidOperationException("All selected rooms must belong to the membership organization");
        }

        Set<Lab> selectedLabs = new LinkedHashSet<>(labs);
        Set<Room> selectedRooms = rooms.stream()
                .filter(room -> !labIds.contains(room.getLab().getId()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AccessSelection(selectedLabs, selectedRooms);
    }

    private record AccessSelection(Set<Lab> labs, Set<Room> rooms) {
    }
}
