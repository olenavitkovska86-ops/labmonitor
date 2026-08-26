package com.olena.labmonitor.membership.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull String role) {
}
