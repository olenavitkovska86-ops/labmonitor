package com.olena.labmonitor.membership.dto;

import com.olena.labmonitor.membership.Membership;

import java.time.LocalDateTime;

public record MembershipResponse(
        Long id,
        Long organizationId,
        Long userId,
        String role,
        LocalDateTime createdAt
) {
    public static MembershipResponse from(Membership membership){


        return new MembershipResponse(
                membership.getId(),
                membership.getOrganization().getId(),
                membership.getUser().getId(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }
}
