package com.olena.labmonitor.analytics.dto;

public record RoomHistoryResponse(
        Long roomId,
        String roomName,
        Long labId,
        String labName,
        long alerts,
        long criticalAlerts,
        long resolvedAlerts
) {
}
