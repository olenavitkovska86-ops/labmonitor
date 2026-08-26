package com.olena.labmonitor.membership.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SaveMembershipRequest(
        @NotNull Long userId,
        @NotNull Long organizationId,
        @NotNull String role,
        @NotNull @Valid MembershipScopeRequest scope
) {
}
