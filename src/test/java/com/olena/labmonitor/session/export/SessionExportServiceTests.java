package com.olena.labmonitor.session.export;

import com.olena.labmonitor.alert.Alert;
import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.alert.AlertSeverity;
import com.olena.labmonitor.alert.AlertStatus;
import com.olena.labmonitor.alert.AlertType;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingStatus;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.event.SessionEvent;
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
        assertTrue(entries.get("readings.csv").contains("safe_min,safe_max,reading_status"));
    }

    @Test
    void rejectsPlannedSession() {
        MonitoringSession session = session(null, null, MonitoringSessionStatus.PLANNED);
        when(sessionService.getSession(10L)).thenReturn(session);

        var error = assertThrows(RuntimeException.class, () -> service.export(10L));

        assertTrue(error.getMessage().contains("must be started"));
        verifyNoInteractions(readingRepository, eventRepository, alertRepository);
    }

    @Test
    void keepsNegativeReadingAndAlertValuesNumeric() throws Exception {
        var start = LocalDateTime.of(2026, 8, 25, 10, 0);
        var end = start.plusHours(1);
        MonitoringSession session = session(start, end, MonitoringSessionStatus.COMPLETED);
        SensorReading reading = reading(start.plusMinutes(5));
        when(reading.getValue()).thenReturn(new BigDecimal("-12.5"));
        when(reading.getSafeMin()).thenReturn(new BigDecimal("-20"));
        when(reading.getSafeMax()).thenReturn(new BigDecimal("-5"));
        Alert alert = mock(Alert.class);
        when(alert.getInitialValue()).thenReturn(new BigDecimal("-12.5"));
        when(alert.getLatestValue()).thenReturn(new BigDecimal("-13"));
        when(alert.getMostExtremeValue()).thenReturn(new BigDecimal("-18.25"));
        stubExport(session, List.of(reading), List.of(), List.of(alert));

        Map<String, String> entries = unzip(service.export(10L).content());

        assertTrue(entries.get("readings.csv").contains("\"-12.5\",\"C\",\"-20\",\"-5\""));
        assertTrue(entries.get("alerts.csv").contains("\"-12.5\",\"-13\",\"-18.25\""));
        assertFalse(entries.get("readings.csv").contains("\"'-"));
        assertFalse(entries.get("alerts.csv").contains("\"'-"));
    }

    @Test
    void activeSessionHasEmptyEndedAtAndUsesCurrentTimeOnlyForQueryBoundary() throws Exception {
        var start = LocalDateTime.now().minusHours(1);
        MonitoringSession session = session(start, null, MonitoringSessionStatus.ACTIVE);
        when(sessionService.getSession(10L)).thenReturn(session);
        when(readingRepository.findForExport(eq(2L), isNull(), eq(start.minusMinutes(15)),
                any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of());
        when(eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(10L)).thenReturn(List.of());
        when(alertRepository.findOverlappingRoomPeriod(eq(2L), eq(start.minusMinutes(15)),
                any(LocalDateTime.class))).thenReturn(List.of());

        String sessionCsv = unzip(service.export(10L).content()).get("session.csv");
        String dataRow = sessionCsv.lines().skip(1).findFirst().orElseThrow();

        List<String> fields = csvFields(dataRow);
        assertEquals("ACTIVE", fields.get(3));
        assertEquals(start.toString(), fields.get(10));
        assertEquals("", fields.get(11));
        assertEquals(start.minusMinutes(15).toString(), fields.get(15));
    }

    @Test
    void rejectsCancelledSessionThatNeverStarted() {
        MonitoringSession session = session(null, LocalDateTime.of(2026, 8, 25, 10, 0),
                MonitoringSessionStatus.CANCELLED);
        when(sessionService.getSession(10L)).thenReturn(session);

        var error = assertThrows(RuntimeException.class, () -> service.export(10L));

        assertTrue(error.getMessage().contains("must be started"));
        verifyNoInteractions(readingRepository, eventRepository, alertRepository);
    }

    @Test
    void protectsUserTextFromCsvFormulas() throws Exception {
        var start = LocalDateTime.of(2026, 8, 25, 10, 0);
        var end = start.plusHours(1);
        MonitoringSession session = session(start, end, MonitoringSessionStatus.COMPLETED);
        when(session.getName()).thenReturn("=HYPERLINK(\"https://example.invalid\")");
        when(session.getDescription()).thenReturn("+malicious formula");
        when(session.getRoom().getName()).thenReturn("@external data");
        SessionEvent event = mock(SessionEvent.class);
        when(event.getTitle()).thenReturn("-dangerous text");
        stubExport(session, List.of(), List.of(event), List.of());

        Map<String, String> entries = unzip(service.export(10L).content());

        assertTrue(entries.get("session.csv").contains("\"'=HYPERLINK(\"\"https://example.invalid\"\")\""));
        assertTrue(entries.get("session.csv").contains("\"'+malicious formula\""));
        assertTrue(entries.get("session.csv").contains("\"'@external data\""));
        assertTrue(entries.get("events.csv").contains("\"'-dangerous text\""));
    }

    @Test
    void enrichesCsvSchemaWithHierarchyIdentifiersAndSessionRelations() throws Exception {
        var start = LocalDateTime.of(2026, 8, 25, 10, 0);
        var end = start.plusHours(1);
        MonitoringSession session = session(start, end, MonitoringSessionStatus.COMPLETED);
        SensorReading reading = reading(start.plusMinutes(5));
        Sensor sensor = reading.getSensor();
        SessionEvent event = mock(SessionEvent.class);
        User eventUser = mock(User.class);
        when(event.getId()).thenReturn(7L);
        when(event.getOccurredAt()).thenReturn(start.plusMinutes(10));
        when(event.getCreatedAt()).thenReturn(start.plusMinutes(11));
        when(event.getCreatedBy()).thenReturn(eventUser);
        when(eventUser.getId()).thenReturn(12L);
        when(eventUser.getFirstName()).thenReturn("Event");
        when(eventUser.getLastName()).thenReturn("Author");

        Alert alert = mock(Alert.class);
        when(alert.getId()).thenReturn(9L);
        when(alert.getSensor()).thenReturn(sensor);
        when(alert.getType()).thenReturn(AlertType.SENSOR_THRESHOLD);
        when(alert.getSeverity()).thenReturn(AlertSeverity.HIGH);
        when(alert.getStatus()).thenReturn(AlertStatus.ACTIVE);
        when(alert.getViolationStartedAt()).thenReturn(start.minusMinutes(5));
        when(alert.getRecoveredAt()).thenReturn(start.plusMinutes(10));
        stubExport(session, List.of(reading), List.of(event), List.of(alert));

        Map<String, String> entries = unzip(service.export(10L).content());

        assertTrue(entries.get("session.csv").startsWith("\uFEFFsession_id,session_name,description,status,"
                + "organization_id,organization_name,lab_id,lab_name,room_id,room_name"));
        assertTrue(entries.get("readings.csv").startsWith("\uFEFFcontext_session_id,reading_id,session_phase,"
                + "organization_id,organization_name,lab_id,lab_name"));
        assertTrue(entries.get("readings.csv").contains("\"10\",\"4\",\"DURING\",\"20\",\"Test organization\","
                + "\"21\",\"Test lab\""));
        assertTrue(entries.get("events.csv").contains("\"10\",\"7\",\"" + start.plusMinutes(10) + "\""));
        assertTrue(entries.get("events.csv").contains("\"12\",\"Event Author\""));
        assertTrue(entries.get("alerts.csv").startsWith("\uFEFFcontext_session_id,alert_id,organization_id,"
                + "organization_name,lab_id,lab_name,room_id,room_name"));
        assertTrue(entries.get("alerts.csv").contains("\"ACTIVE\",\"RECOVERED\",\""
                + start.minusMinutes(5) + "\",\"" + start.plusMinutes(10)
                + "\",\"BEFORE\",\"DURING\",\"true\""));
    }

    private void stubExport(MonitoringSession session, List<SensorReading> readings,
                            List<SessionEvent> events, List<Alert> alerts) {
        LocalDateTime exportFrom = session.getStartedAt().minusMinutes(15);
        LocalDateTime exportTo = session.getEndedAt().plusMinutes(15);
        when(sessionService.getSession(10L)).thenReturn(session);
        when(readingRepository.findForExport(eq(2L), isNull(), eq(exportFrom), eq(exportTo),
                any(Pageable.class))).thenReturn(readings);
        when(eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(10L)).thenReturn(events);
        when(alertRepository.findOverlappingRoomPeriod(2L, exportFrom, exportTo)).thenReturn(alerts);
    }

    private MonitoringSession session(LocalDateTime start, LocalDateTime end, MonitoringSessionStatus status) {
        MonitoringSession session = mock(MonitoringSession.class);
        Room room = mock(Room.class);
        Lab lab = mock(Lab.class);
        Organization organization = mock(Organization.class);
        User user = mock(User.class);
        when(session.getId()).thenReturn(10L);
        when(session.getName()).thenReturn("Cooling test");
        when(session.getDescription()).thenReturn("Test export");
        when(session.getStatus()).thenReturn(status);
        when(session.getStartedAt()).thenReturn(start);
        when(session.getEndedAt()).thenReturn(end);
        when(session.getRoom()).thenReturn(room);
        when(session.getCreatedBy()).thenReturn(user);
        when(session.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 25, 8, 0));
        when(room.getId()).thenReturn(2L);
        when(room.getName()).thenReturn("Server room");
        when(room.getLab()).thenReturn(lab);
        when(lab.getId()).thenReturn(21L);
        when(lab.getName()).thenReturn("Test lab");
        when(lab.getOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(20L);
        when(organization.getName()).thenReturn("Test organization");
        when(user.getId()).thenReturn(11L);
        when(user.getFirstName()).thenReturn("Test");
        when(user.getLastName()).thenReturn("User");
        return session;
    }

    private SensorReading reading(LocalDateTime measuredAt) {
        SensorReading reading = mock(SensorReading.class);
        Sensor sensor = mock(Sensor.class);
        Room room = mock(Room.class);
        when(reading.getMeasuredAt()).thenReturn(measuredAt);
        when(reading.getId()).thenReturn(4L);
        when(reading.getCreatedAt()).thenReturn(measuredAt.plusSeconds(1));
        when(reading.getSensor()).thenReturn(sensor);
        when(reading.getRoom()).thenReturn(room);
        when(reading.getValue()).thenReturn(new BigDecimal("21.5"));
        when(reading.getSafeMin()).thenReturn(new BigDecimal("18"));
        when(reading.getSafeMax()).thenReturn(new BigDecimal("25"));
        when(reading.getStatus()).thenReturn(SensorReadingStatus.SAFE);
        when(sensor.getId()).thenReturn(3L);
        when(sensor.getName()).thenReturn("Temperature");
        when(sensor.getType()).thenReturn(SensorType.TEMPERATURE);
        when(sensor.getUnit()).thenReturn("C");
        when(room.getId()).thenReturn(2L);
        when(room.getName()).thenReturn("Server room");
        return reading;
    }

    private List<String> csvFields(String row) {
        List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < row.length(); index++) {
            char character = row.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        fields.add(field.toString());
        return fields;
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
