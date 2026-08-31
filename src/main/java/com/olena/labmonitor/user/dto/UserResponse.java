package com.olena.labmonitor.user.dto;

import com.olena.labmonitor.user.User;
import com.olena.labmonitor.membership.MembershipScopeType;
import com.olena.labmonitor.security.Permissions;

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
        boolean alertNotificationsEnabled,
        List<String> permissions,
        List<MembershipInfo> memberships,
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
                user.isAlertNotificationsEnabled(),
                Permissions.global(user.getGlobalRole()),
                user.getMemberships().stream()
                        .map(membership -> new MembershipInfo(
                                membership.getId(),
                                membership.getOrganization().getId(),
                                membership.getOrganization().getName(),
                                membership.getRole(),
                                Permissions.organization(membership.getRole()),
                                membership.getScopeType(),
                                membership.getAccessibleLabs().stream().map(lab -> lab.getId()).toList(),
                                membership.getAccessibleRooms().stream().map(room -> room.getId()).toList(),
                                membership.getAccessibleLabs().stream().map(lab -> lab.getName()).toList(),
                                membership.getAccessibleRooms().stream().map(room -> room.getName()).toList()
                        ))
                        .toList(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public record MembershipInfo(
            Long id,
            Long organizationId,
            String organizationName,
            String role,
            List<String> permissions,
            MembershipScopeType scopeType,
            List<Long> labIds,
            List<Long> roomIds,
            List<String> labNames,
            List<String> roomNames
    ){}
}
