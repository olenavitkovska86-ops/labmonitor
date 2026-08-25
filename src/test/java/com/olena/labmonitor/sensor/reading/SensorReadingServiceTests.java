package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.alert.AlertService;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorService;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorReadingServiceTests {

    private static final LocalDateTime RECEIVED_AT = LocalDateTime.of(2026, 8, 25, 12, 0);

    @Mock
    private SensorReadingRepository repository;
    @Mock
    private SensorService sensorService;
    @Mock
    private AlertService alertService;

    private SensorReadingService service;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        service = new SensorReadingService(repository, sensorService, alertService, new MonitoringProperties());
        Room room = new Room(new Lab(new Organization("Test organization", null), "Test lab", null, null),
                "Test room", RoomType.EXPERIMENT_ROOM, null, null);
        sensor = new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
        sensor.updateSafeRange(new BigDecimal("18"), new BigDecimal("25"));
        ReflectionTestUtils.setField(sensor, "id", 3L);
        when(sensorService.getExistingSensor(3L)).thenReturn(sensor);
    }

    @Test
    void newestReadingUpdatesThresholdLifecycle() {
        LocalDateTime measuredAt = RECEIVED_AT.minusSeconds(1);
        SensorReading saved = savedReading(11L, "26", measuredAt);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(SensorReading.class))).thenReturn(saved);
        when(repository.findFirstBySensorIdOrderByMeasuredAtDescIdDesc(3L)).thenReturn(Optional.of(saved));

        service.create(new CreateSensorReadingRequest(3L, new BigDecimal("26"), measuredAt));

        verify(alertService).processSensorOnline(sensor, RECEIVED_AT);
        verify(alertService).processThresholdReading(sensor, new BigDecimal("26"), measuredAt);
        assertEquals(RECEIVED_AT, sensor.getLastSeenAt());
    }

    @Test
    void lateReadingIsStoredAndUpdatesLivenessButDoesNotChangeThresholdLifecycle() {
        LocalDateTime lateMeasuredAt = RECEIVED_AT.minusHours(2);
        SensorReading savedLate = savedReading(11L, "30", lateMeasuredAt);
        SensorReading current = savedReading(10L, "22", RECEIVED_AT.minusMinutes(1));
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(SensorReading.class))).thenReturn(savedLate);
        when(repository.findFirstBySensorIdOrderByMeasuredAtDescIdDesc(3L)).thenReturn(Optional.of(current));

        service.create(new CreateSensorReadingRequest(3L, new BigDecimal("30"), lateMeasuredAt));

        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(SensorReading.class));
        verify(alertService).processSensorOnline(sensor, RECEIVED_AT);
        verify(alertService, never()).processThresholdReading(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertEquals(RECEIVED_AT, sensor.getLastSeenAt());
    }

    @Test
    void laterIdWinsWhenMeasurementsHaveSameTimestamp() {
        LocalDateTime measuredAt = RECEIVED_AT.minusMinutes(1);
        SensorReading saved = savedReading(12L, "26", measuredAt);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(SensorReading.class))).thenReturn(saved);
        when(repository.findFirstBySensorIdOrderByMeasuredAtDescIdDesc(3L)).thenReturn(Optional.of(saved));

        service.create(new CreateSensorReadingRequest(3L, new BigDecimal("26"), measuredAt));

        verify(alertService).processThresholdReading(sensor, new BigDecimal("26"), measuredAt);
    }

    private SensorReading savedReading(Long id, String value, LocalDateTime measuredAt) {
        SensorReading reading = new SensorReading(sensor, new BigDecimal(value), measuredAt);
        ReflectionTestUtils.setField(reading, "id", id);
        ReflectionTestUtils.setField(reading, "createdAt", RECEIVED_AT);
        return reading;
    }
}
