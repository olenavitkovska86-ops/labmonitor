package com.olena.labmonitor.session.dto;

import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionStatus;

import java.time.LocalDateTime;

public record MonitoringSessionResponse(
        Long id,
        Long roomId,
        String roomName,
        String name,
        String description,
        MonitoringSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long createdByUserId,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MonitoringSessionResponse from(MonitoringSession session) {
        var creator = session.getCreatedBy();
        return new MonitoringSessionResponse(
                session.getId(), session.getRoom().getId(), session.getRoom().getName(),
                session.getName(), session.getDescription(), session.getStatus(),
                session.getStartedAt(), session.getEndedAt(), creator.getId(),
                creator.getFirstName() + " " + creator.getLastName(),
                session.getCreatedAt(), session.getUpdatedAt()
        );
    }
}
