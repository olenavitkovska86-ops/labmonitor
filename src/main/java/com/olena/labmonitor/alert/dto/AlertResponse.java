package com.olena.labmonitor.alert.dto;

import com.olena.labmonitor.alert.*;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        Long organizationId,
        Long labId,
        Long roomId,
        Long sensorId,
        AlertType type,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String message,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        Long acknowledgedByUserId,
        LocalDateTime resolvedAt,
        Long resolvedByUserId
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getRoom().getLab().getOrganization().getId(),
                alert.getRoom().getLab().getId(),
                alert.getRoom().getId(),
                alert.getSensor() == null ? null : alert.getSensor().getId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getAcknowledgedByUserId(),
                alert.getResolvedAt(),
                alert.getResolvedByUserId()
        );
    }
}
