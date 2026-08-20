package com.olena.labmonitor.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config/monitoring")
public class MonitoringConfigController {

    private final MonitoringProperties properties;

    public MonitoringConfigController(MonitoringProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public MonitoringConfigResponse getConfiguration() {
        return new MonitoringConfigResponse(
                properties.getReadings().getHistoryDefaultPeriod().toHours(),
                properties.getReadings().getHistoryMaxResults(),
                properties.getReadings().getHistoryPeriods().stream().map(java.time.Duration::toHours).toList(),
                properties.getSensors().getOfflineAfter().toSeconds()
        );
    }

    public record MonitoringConfigResponse(
            long defaultHistoryHours,
            int historyMaxResults,
            List<Long> historyPeriodsHours,
            long sensorOfflineAfterSeconds
    ) {
    }
}
