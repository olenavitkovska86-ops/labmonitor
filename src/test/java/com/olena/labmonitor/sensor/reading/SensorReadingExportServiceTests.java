package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorService;
import com.olena.labmonitor.sensor.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorReadingExportServiceTests {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 21, 10, 0);
    private static final LocalDateTime TO = FROM.plusHours(1);

    @Mock
    private SensorReadingRepository repository;
    @Mock
    private RoomService roomService;
    @Mock
    private SensorService sensorService;

    private SensorReadingExportService service;
    private Room room;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        service = new SensorReadingExportService(
                repository, roomService, sensorService, new MonitoringProperties()
        );
        Organization organization = new Organization("Research, Inc.", null);
        Lab lab = new Lab(organization, "Biology", null, null);
        room = new Room(lab, "Cold \"Room\"", RoomType.EXPERIMENT_ROOM, null, null);
        sensor = new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
        sensor.updateSafeRange(new BigDecimal("18"), new BigDecimal("25"));
        ReflectionTestUtils.setField(room, "id", 1L);
        ReflectionTestUtils.setField(sensor, "id", 2L);
    }

    @Test
    void exportsLongCsvWithSafeAndOutsideRangeRows() {
        SensorReading safe = reading("22.4", FROM.plusMinutes(1));
        SensorReading unsafe = reading("26.1", FROM.plusMinutes(2));
        sensor.updateSafeRange(new BigDecimal("10"), new BigDecimal("30"));
        when(roomService.getExistingRoom(1L)).thenReturn(room);
        when(sensorService.getExistingSensor(2L)).thenReturn(sensor);
        when(repository.findForExport(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(safe, unsafe));

        var export = service.export(1L, 2L, FROM, TO);
        String csv = new String(export.content(), StandardCharsets.UTF_8);

        assertEquals("sensor-2-readings.csv", export.filename());
        assertTrue(csv.startsWith("\uFEFFmeasured_at,received_at,room_id,room,sensor_id,sensor,"));
        assertTrue(csv.contains("\"Cold \"\"Room\"\"\""));
        assertTrue(csv.contains("\"18\",\"25\",SAFE"));
        assertTrue(csv.contains("\"18\",\"25\",OUTSIDE_RANGE"));
        assertTrue(csv.contains("SAFE\n"));
        assertTrue(csv.contains("OUTSIDE_RANGE\n"));
    }

    @Test
    void rejectsPeriodLongerThanConfiguredMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> service.export(1L, null, FROM, FROM.plusDays(31)));

        verify(repository, never()).findForExport(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void rejectsSensorFromAnotherRoom() {
        Room anotherRoom = new Room(room.getLab(), "Other", RoomType.EXPERIMENT_ROOM, null, null);
        ReflectionTestUtils.setField(anotherRoom, "id", 3L);
        Sensor anotherSensor = new Sensor(anotherRoom, "Humidity", SensorType.HUMIDITY, "%");
        ReflectionTestUtils.setField(anotherSensor, "id", 4L);
        when(roomService.getExistingRoom(1L)).thenReturn(room);
        when(sensorService.getExistingSensor(4L)).thenReturn(anotherSensor);

        assertThrows(IllegalArgumentException.class,
                () -> service.export(1L, 4L, FROM, TO));
    }

    private SensorReading reading(String value, LocalDateTime measuredAt) {
        SensorReading reading = new SensorReading(sensor, new BigDecimal(value), measuredAt);
        ReflectionTestUtils.setField(reading, "createdAt", measuredAt.plusSeconds(1));
        return reading;
    }
}
