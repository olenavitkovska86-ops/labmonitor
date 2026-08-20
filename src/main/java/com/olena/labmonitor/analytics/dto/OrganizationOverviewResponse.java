package com.olena.labmonitor.analytics.dto;

import java.time.LocalDateTime;

public record OrganizationOverviewResponse(
        Long organizationId,
        String organizationName,
        LocalDateTime generatedAt,
        long totalRooms,
        long roomsRequiringAttention,
        long unresolvedAlerts,
        long unacknowledgedAlerts,
        long criticalAlerts,
        long offlineSensors
) {
}
