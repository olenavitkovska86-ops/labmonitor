package com.olena.labmonitor.security;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipScopeType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessPolicy {
    private final UserRepository userRepository;

    public AccessPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserAccess forAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        Map<Long, OrganizationGrant> grants = new LinkedHashMap<>();
        for (Membership membership : user.getMemberships()) {
            Set<Long> labIds = membership.getAccessibleLabs().stream()
                    .map(lab -> lab.getId())
                    .collect(Collectors.toUnmodifiableSet());
            Map<Long, Long> roomLabs = membership.getAccessibleRooms().stream()
                    .collect(Collectors.toUnmodifiableMap(room -> room.getId(), room -> room.getLab().getId()));
            grants.put(membership.getOrganization().getId(), new OrganizationGrant(
                    membership.getRole(), membership.getScopeType(), labIds, roomLabs));
        }
        return new UserAccess("SUPER_ADMIN".equals(user.getGlobalRole()), Map.copyOf(grants));
    }

    public record UserAccess(boolean superAdmin, Map<Long, OrganizationGrant> organizations) {
        public boolean canViewOrganization(Long organizationId) {
            return superAdmin || organizations.containsKey(organizationId);
        }

        public boolean hasOrganizationWideAccess(Long organizationId) {
            if (superAdmin) return true;
            OrganizationGrant grant = organizations.get(organizationId);
            return grant != null && grant.scopeType() == MembershipScopeType.ORGANIZATION;
        }

        public boolean canViewLab(Long organizationId, Long labId) {
            if (superAdmin) return true;
            OrganizationGrant grant = organizations.get(organizationId);
            return grant != null && grant.includesLab(labId);
        }

        public boolean canViewRoom(Long organizationId, Long labId, Long roomId) {
            if (superAdmin) return true;
            OrganizationGrant grant = organizations.get(organizationId);
            return grant != null && grant.includesRoom(labId, roomId);
        }

        public boolean canManageSensor(Long organizationId, Long labId, Long roomId) {
            if (superAdmin) return true;
            OrganizationGrant grant = organizations.get(organizationId);
            return grant != null && "LAB_ADMIN".equals(grant.role()) && grant.includesRoom(labId, roomId);
        }

        public boolean canManageAlert(Long organizationId, Long labId, Long roomId) {
            return canManageRoomResource(organizationId, labId, roomId, Permissions.ALERTS_MANAGE);
        }

        public boolean canManageSession(Long organizationId, Long labId, Long roomId) {
            return canManageRoomResource(organizationId, labId, roomId, Permissions.SESSIONS_MANAGE);
        }

        public void requireViewOrganization(Long organizationId) {
            require(canViewOrganization(organizationId));
        }

        public void requireViewLab(Long organizationId, Long labId) {
            require(canViewLab(organizationId, labId));
        }

        public void requireViewRoom(Long organizationId, Long labId, Long roomId) {
            require(canViewRoom(organizationId, labId, roomId));
        }

        public void requireManageSensor(Long organizationId, Long labId, Long roomId) {
            require(canManageSensor(organizationId, labId, roomId));
        }

        public void requireManageAlert(Long organizationId, Long labId, Long roomId) {
            require(canManageAlert(organizationId, labId, roomId));
        }

        public void requireManageSession(Long organizationId, Long labId, Long roomId) {
            require(canManageSession(organizationId, labId, roomId));
        }

        private boolean canManageRoomResource(Long organizationId, Long labId, Long roomId, String permission) {
            if (superAdmin) return true;
            OrganizationGrant grant = organizations.get(organizationId);
            return grant != null
                    && Permissions.organization(grant.role()).contains(permission)
                    && grant.includesRoom(labId, roomId);
        }

        private void require(boolean allowed) {
            if (!allowed) throw new AccessDeniedException("You do not have access to this resource");
        }
    }

    public record OrganizationGrant(String role, MembershipScopeType scopeType,
                                    Set<Long> labIds, Map<Long, Long> roomLabs) {
        boolean includesLab(Long labId) {
            return scopeType == MembershipScopeType.ORGANIZATION
                    || labIds.contains(labId)
                    || roomLabs.containsValue(labId);
        }

        boolean includesRoom(Long labId, Long roomId) {
            return scopeType == MembershipScopeType.ORGANIZATION
                    || labIds.contains(labId)
                    || roomLabs.containsKey(roomId);
        }
    }
}
