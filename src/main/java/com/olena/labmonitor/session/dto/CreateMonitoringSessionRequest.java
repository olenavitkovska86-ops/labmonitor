package com.olena.labmonitor.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMonitoringSessionRequest(
        @NotNull(message = "Room id is required") Long roomId,
        @NotBlank(message = "Session name is required")
        @Size(max = 150, message = "Session name must not be longer than 150 characters") String name,
        @Size(max = 1000, message = "Session description must not be longer than 1000 characters") String description
) {
}
