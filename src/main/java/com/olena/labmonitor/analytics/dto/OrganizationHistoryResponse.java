package com.olena.labmonitor.analytics.dto;

import com.olena.labmonitor.analytics.AnalyticsPeriod;

import java.time.LocalDateTime;
import java.util.List;

public record OrganizationHistoryResponse(
        Long organizationId,
        AnalyticsPeriod period,
        LocalDateTime from,
        LocalDateTime to,
        long alertsCreated,
        long criticalAlerts,
        long resolvedAlerts,
        Long averageAcknowledgementMinutes,
        Long averageResolutionMinutes,
        List<DailyAlertCountResponse> dailyAlerts,
        List<RoomHistoryResponse> mostProblematicRooms
) {
}
