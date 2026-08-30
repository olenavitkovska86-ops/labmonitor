package com.olena.labmonitor.user.dto;

import com.olena.labmonitor.membership.dto.AccessAssignmentResponse;
import com.olena.labmonitor.user.User;

import java.util.List;
import java.util.Set;

public record ManagedUserResponse(
        Long id, String firstName, String lastName, String email,
        String status, String globalRole,
        List<AccessAssignmentResponse> assignments,
        Set<String> allowedActions
) {
    public static ManagedUserResponse forSuperAdmin(User user) {
        Set<String> actions = "SUPER_ADMIN".equals(user.getGlobalRole())
                ? Set.of("ACCOUNT_STATUS_UPDATE")
                : Set.of("ACCOUNT_STATUS_UPDATE", "ACCESS_ADD", "ACCESS_EDIT", "ACCESS_REMOVE");
        return new ManagedUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getStatus(), user.getGlobalRole(),
                user.getMemberships().stream().map(AccessAssignmentResponse::from).toList(), actions);
    }

    public static ManagedUserResponse forLabAdmin(User user, AccessAssignmentResponse assignment) {
        return new ManagedUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                null, null, List.of(assignment), Set.of("ACCESS_EDIT"));
    }
}
