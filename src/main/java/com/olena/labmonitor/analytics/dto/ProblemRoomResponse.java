package com.olena.labmonitor.analytics.dto;

import com.olena.labmonitor.alert.AlertSeverity;
import com.olena.labmonitor.alert.AlertStatus;
import com.olena.labmonitor.analytics.AttentionLevel;

import java.time.LocalDateTime;

public record ProblemRoomResponse(
        Long roomId,
        String roomName,
        Long labId,
        String labName,
        AttentionLevel attentionLevel,
        long unresolvedAlerts,
        long unacknowledgedAlerts,
        long criticalAlerts,
        Long mainAlertId,
        String mainProblem,
        AlertSeverity mainProblemSeverity,
        AlertStatus mainProblemStatus,
        LocalDateTime problemStartedAt,
        long openMinutes
) {
}
