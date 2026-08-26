package com.olena.labmonitor.membership.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRequest(
        @NotNull String role,
        @NotNull @Valid MembershipScopeRequest scope
) {
}
