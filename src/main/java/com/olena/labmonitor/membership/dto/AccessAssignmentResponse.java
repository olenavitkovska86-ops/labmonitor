package com.olena.labmonitor.membership.dto;

import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipScopeType;

import java.util.Set;
import java.util.stream.Collectors;

public record AccessAssignmentResponse(
        Long membershipId, Long organizationId, String organizationName, String role,
        MembershipScopeType scopeType, Set<Long> labIds, Set<Long> roomIds
) {
    public static AccessAssignmentResponse from(Membership membership) {
        return from(membership,
                membership.getAccessibleLabs().stream().map(lab -> lab.getId()).collect(Collectors.toUnmodifiableSet()),
                membership.getAccessibleRooms().stream().map(room -> room.getId()).collect(Collectors.toUnmodifiableSet()));
    }

    public static AccessAssignmentResponse from(Membership membership, Set<Long> labIds, Set<Long> roomIds) {
        return new AccessAssignmentResponse(membership.getId(), membership.getOrganization().getId(),
                membership.getOrganization().getName(), membership.getRole(), membership.getScopeType(), labIds, roomIds);
    }
}
