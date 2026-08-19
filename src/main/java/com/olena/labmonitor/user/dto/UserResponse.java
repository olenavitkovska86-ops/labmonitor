package com.olena.labmonitor.user.dto;

import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.user.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String status,
        String globalRole,
        List<Membership> membership, // Mapped by UserMapper
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getGlobalRole(),
                user.getMemberships(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // TODO: Fix the organization id thingy with claud
    public record MembershipInfo(
            Long organizationId,
            String organizationName,
            String role
    ){}
}
