package com.olena.labmonitor.user.dto;

import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DemoteRequest(
        @NotNull String role,
        @NotNull Long organizationId,
        @NotNull @Valid MembershipScopeRequest scope
) {
}
