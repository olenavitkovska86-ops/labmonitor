package com.olena.labmonitor.membership.dto;

import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipScopeType;

import java.util.Set;
import java.util.stream.Collectors;

public record MembershipResponse(
        Long id,
        Long userId,
        Long organizationId,
        String organizationName,
        String role,
        MembershipScopeType scopeType,
        Set<Long> labIds,
        Set<Long> roomIds
) {
    public static MembershipResponse from(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getRole(),
                membership.getScopeType(),
                membership.getAccessibleLabs().stream().map(lab -> lab.getId()).collect(Collectors.toUnmodifiableSet()),
                membership.getAccessibleRooms().stream().map(room -> room.getId()).collect(Collectors.toUnmodifiableSet())
        );
    }
}
