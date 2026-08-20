package com.olena.labmonitor.sensor;

import com.olena.labmonitor.alert.AlertService;
import com.olena.labmonitor.config.MonitoringProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class SensorAvailabilityMonitor {

    private final SensorRepository sensorRepository;
    private final AlertService alertService;
    private final MonitoringProperties monitoringProperties;

    public SensorAvailabilityMonitor(
            SensorRepository sensorRepository,
            AlertService alertService,
            MonitoringProperties monitoringProperties
    ) {
        this.sensorRepository = sensorRepository;
        this.alertService = alertService;
        this.monitoringProperties = monitoringProperties;
    }

    @Scheduled(
            fixedDelayString = "${labmonitor.monitoring.sensors.offline-check-interval:10s}",
            initialDelayString = "${labmonitor.monitoring.sensors.offline-check-interval:10s}"
    )
    @Transactional
    public void checkSensors() {
        checkSensorsAt(LocalDateTime.now());
    }

    void checkSensorsAt(LocalDateTime now) {
        LocalDateTime cutoff = now.minus(monitoringProperties.getSensors().getOfflineAfter());
        for (Sensor sensor : sensorRepository.findSensorsMissingSince(cutoff)) {
            sensor.markOffline();
            alertService.processSensorOffline(sensor, now);
        }
    }
}
