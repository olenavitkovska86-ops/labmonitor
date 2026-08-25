package com.olena.labmonitor.session.event.dto;

import com.olena.labmonitor.session.event.SessionEvent;
import com.olena.labmonitor.session.event.SessionEventCategory;

import java.time.LocalDateTime;

public record SessionEventResponse(
        Long id,
        Long sessionId,
        SessionEventCategory category,
        String title,
        String description,
        LocalDateTime occurredAt,
        Long createdByUserId,
        String createdByName,
        LocalDateTime createdAt
) {
    public static SessionEventResponse from(SessionEvent event) {
        var creator = event.getCreatedBy();
        return new SessionEventResponse(event.getId(), event.getSession().getId(), event.getCategory(),
                event.getTitle(), event.getDescription(), event.getOccurredAt(), creator.getId(),
                creator.getFirstName() + " " + creator.getLastName(), event.getCreatedAt());
    }
}
