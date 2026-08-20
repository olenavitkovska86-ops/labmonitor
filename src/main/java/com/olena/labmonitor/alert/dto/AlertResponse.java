package com.olena.labmonitor.alert.dto;

import com.olena.labmonitor.alert.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        String acknowledgedByName,
        LocalDateTime resolvedAt,
        Long resolvedByUserId,
        String resolvedByName,
        AlertResolutionOutcome resolutionOutcome,
        String resolutionComment,
        LocalDateTime reopenedAt,
        Long reopenedByUserId,
        String reopenedByName,
        LocalDateTime violationStartedAt,
        BigDecimal initialValue,
        BigDecimal latestValue,
        BigDecimal mostExtremeValue,
        LocalDateTime lastViolationAt,
        LocalDateTime recoveredAt
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
                alert.getAcknowledgedByUser() == null ? null : alert.getAcknowledgedByUser().getId(),
                displayName(alert.getAcknowledgedByUser()),
                alert.getResolvedAt(),
                alert.getResolvedByUser() == null ? null : alert.getResolvedByUser().getId(),
                displayName(alert.getResolvedByUser()),
                alert.getResolutionOutcome(),
                alert.getResolutionComment(),
                alert.getReopenedAt(),
                alert.getReopenedByUser() == null ? null : alert.getReopenedByUser().getId(),
                displayName(alert.getReopenedByUser()),
                alert.getViolationStartedAt(),
                alert.getInitialValue(),
                alert.getLatestValue(),
                alert.getMostExtremeValue(),
                alert.getLastViolationAt(),
                alert.getRecoveredAt()
        );
    }

    private static String displayName(com.olena.labmonitor.user.User user) {
        if (user == null) {
            return null;
        }
        String name = (user.getFirstName() + " " + user.getLastName()).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
