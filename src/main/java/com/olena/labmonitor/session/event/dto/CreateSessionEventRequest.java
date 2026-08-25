package com.olena.labmonitor.session.event.dto;

import com.olena.labmonitor.session.event.SessionEventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateSessionEventRequest(
        @NotNull(message = "Event category is required") SessionEventCategory category,
        @NotBlank(message = "Event title is required")
        @Size(max = 150, message = "Event title must not be longer than 150 characters") String title,
        @Size(max = 1000, message = "Event description must not be longer than 1000 characters") String description,
        @NotNull(message = "Event time is required") LocalDateTime occurredAt
) {
}
