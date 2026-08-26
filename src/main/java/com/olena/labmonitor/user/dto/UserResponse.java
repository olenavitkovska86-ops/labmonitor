package com.olena.labmonitor.user.dto;

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
        List<MembershipInfo> memberships, // Mapped by UserMapper
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user){
        List<MembershipInfo> memberships =
                user.getMemberships().stream()
                        .map(m -> new MembershipInfo(
                                m.getOrganization().getId(),
                                m.getOrganization().getName(),
                                m.getRole()
                        )).toList();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getGlobalRole(),
                memberships,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public record MembershipInfo(
            Long organizationId,
            String organizationName,
            String role
    ){}
}
