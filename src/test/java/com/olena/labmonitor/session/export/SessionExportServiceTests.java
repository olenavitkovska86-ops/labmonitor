package com.olena.labmonitor.session.export;

import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingStatus;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.event.SessionEventRepository;
import com.olena.labmonitor.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionExportServiceTests {
    private final MonitoringSessionService sessionService = mock(MonitoringSessionService.class);
    private final SensorReadingRepository readingRepository = mock(SensorReadingRepository.class);
    private final SessionEventRepository eventRepository = mock(SessionEventRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final SessionExportService service = new SessionExportService(sessionService, readingRepository,
            eventRepository, alertRepository, new MonitoringProperties());

    @Test
    void exportsFourCsvFilesAndLabelsReadingPhases() throws Exception {
        var start = LocalDateTime.of(2026, 8, 25, 10, 0);
        var end = start.plusHours(1);
        MonitoringSession session = session(start, end, MonitoringSessionStatus.COMPLETED);
        List<SensorReading> readings = List.of(
                reading(start.minusMinutes(5)), reading(start.plusMinutes(30)), reading(end.plusMinutes(5)));
        when(sessionService.getSession(10L)).thenReturn(session);
        when(readingRepository.findForExport(eq(2L), isNull(), eq(start.minusMinutes(15)),
                eq(end.plusMinutes(15)), any(Pageable.class))).thenReturn(readings);
        when(eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(10L)).thenReturn(List.of());
        when(alertRepository.findOverlappingRoomPeriod(2L, start.minusMinutes(15), end.plusMinutes(15)))
                .thenReturn(List.of());

        var export = service.export(10L);
        Map<String, String> entries = unzip(export.content());

        assertEquals("session-10-export.zip", export.filename());
        assertEquals(List.of("session.csv", "readings.csv", "events.csv", "alerts.csv"),
                entries.keySet().stream().toList());
        assertTrue(entries.get("readings.csv").contains("\"BEFORE\""));
        assertTrue(entries.get("readings.csv").contains("\"DURING\""));
        assertTrue(entries.get("readings.csv").contains("\"AFTER\""));
        assertTrue(entries.get("readings.csv").contains("safe_min,safe_max,status"));
    }

    @Test
    void rejectsPlannedSession() {
        MonitoringSession session = session(null, null, MonitoringSessionStatus.PLANNED);
        when(sessionService.getSession(10L)).thenReturn(session);

        var error = assertThrows(RuntimeException.class, () -> service.export(10L));

        assertTrue(error.getMessage().contains("must be started"));
        verifyNoInteractions(readingRepository, eventRepository, alertRepository);
    }

    private MonitoringSession session(LocalDateTime start, LocalDateTime end, MonitoringSessionStatus status) {
        MonitoringSession session = mock(MonitoringSession.class);
        Room room = mock(Room.class);
        User user = mock(User.class);
        when(session.getId()).thenReturn(10L);
        when(session.getName()).thenReturn("Cooling test");
        when(session.getDescription()).thenReturn("Test export");
        when(session.getStatus()).thenReturn(status);
        when(session.getStartedAt()).thenReturn(start);
        when(session.getEndedAt()).thenReturn(end);
        when(session.getRoom()).thenReturn(room);
        when(session.getCreatedBy()).thenReturn(user);
        when(room.getId()).thenReturn(2L);
        when(room.getName()).thenReturn("Server room");
        when(user.getFirstName()).thenReturn("Test");
        when(user.getLastName()).thenReturn("User");
        return session;
    }

    private SensorReading reading(LocalDateTime measuredAt) {
        SensorReading reading = mock(SensorReading.class);
        Sensor sensor = mock(Sensor.class);
        Room room = mock(Room.class);
        when(reading.getMeasuredAt()).thenReturn(measuredAt);
        when(reading.getCreatedAt()).thenReturn(measuredAt.plusSeconds(1));
        when(reading.getSensor()).thenReturn(sensor);
        when(reading.getRoom()).thenReturn(room);
        when(reading.getValue()).thenReturn(new BigDecimal("21.5"));
        when(reading.getSafeMin()).thenReturn(new BigDecimal("18"));
        when(reading.getSafeMax()).thenReturn(new BigDecimal("25"));
        when(reading.getStatus()).thenReturn(SensorReadingStatus.SAFE);
        when(sensor.getId()).thenReturn(3L);
        when(sensor.getName()).thenReturn("Temperature");
        when(sensor.getUnit()).thenReturn("C");
        when(room.getId()).thenReturn(2L);
        when(room.getName()).thenReturn("Server room");
        return reading;
    }

    private Map<String, String> unzip(byte[] content) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                result.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return result;
    }
}
