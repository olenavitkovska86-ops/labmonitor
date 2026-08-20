package com.olena.labmonitor.sensor;

import com.olena.labmonitor.alert.AlertService;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorAvailabilityMonitorTests {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private AlertService alertService;

    @Test
    void marksMissingSensorOfflineAndProcessesAlert() {
        MonitoringProperties properties = new MonitoringProperties();
        properties.getSensors().setOfflineAfter(Duration.ofMinutes(2));
        SensorAvailabilityMonitor monitor = new SensorAvailabilityMonitor(
                sensorRepository, alertService, properties
        );
        Sensor sensor = sensor();
        sensor.recordReading(LocalDateTime.of(2026, 8, 20, 11, 55));
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 12, 0);
        when(sensorRepository.findSensorsMissingSince(now.minusMinutes(2))).thenReturn(List.of(sensor));

        monitor.checkSensorsAt(now);

        assertEquals(SensorStatus.OFFLINE, sensor.getStatus());
        verify(alertService).processSensorOffline(sensor, now);
    }

    private Sensor sensor() {
        Organization organization = new Organization("Test organization", null);
        Lab lab = new Lab(organization, "Test lab", null, null);
        Room room = new Room(lab, "Test room", RoomType.EXPERIMENT_ROOM, null, null);
        return new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
    }
}
