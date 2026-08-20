package com.olena.labmonitor.analytics.dto;

import java.time.LocalDate;

public record DailyAlertCountResponse(
        LocalDate date,
        long alerts,
        long criticalAlerts
) {
}
