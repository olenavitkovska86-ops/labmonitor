package com.olena.labmonitor.session.timeline;

import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.event.SessionEventRepository;
import com.olena.labmonitor.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionTimelineServiceTests {
    private final MonitoringSessionService sessionService = mock(MonitoringSessionService.class);
    private final SensorReadingRepository readingRepository = mock(SensorReadingRepository.class);
    private final SessionEventRepository eventRepository = mock(SessionEventRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final SessionTimelineService service = new SessionTimelineService(
            sessionService, readingRepository, eventRepository, alertRepository);

    @Test
    void returnsReadingsEventsAndAlertsInsideCompletedSessionWindow() {
        MonitoringSession session = session(MonitoringSessionStatus.COMPLETED);
        Sensor sensor = new Sensor(session.getRoom(), "Temperature", SensorType.TEMPERATURE, "°C");
        ReflectionTestUtils.setField(sensor, "id", 20L);
        SensorReading reading = new SensorReading(sensor, new BigDecimal("22.5"),
                LocalDateTime.of(2026, 8, 25, 9, 30));
        ReflectionTestUtils.setField(reading, "id", 30L);
        when(sessionService.getSession(10L)).thenReturn(session);
        when(readingRepository.findSampledForTimeline(eq(1L), isNull(), any(), any(), eq(200)))
                .thenReturn(List.of(reading));
        when(readingRepository.countForTimeline(eq(1L), isNull(), any(), any())).thenReturn(1L);
        when(eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(10L)).thenReturn(List.of());
        when(alertRepository.findOverlappingRoomPeriod(eq(1L), any(), any())).thenReturn(List.of());

        var timeline = service.getTimeline(10L, null);

        assertEquals(LocalDateTime.of(2026, 8, 25, 9, 0), timeline.from());
        assertEquals(LocalDateTime.of(2026, 8, 25, 10, 0), timeline.to());
        assertEquals(1, timeline.readings().size());
        assertEquals("Temperature", timeline.readings().getFirst().sensorName());
        assertFalse(timeline.readingsTruncated());
    }

    @Test
    void rejectsTimelineForSessionThatHasNotStarted() {
        when(sessionService.getSession(10L)).thenReturn(session(MonitoringSessionStatus.PLANNED));

        assertThrows(InvalidOperationException.class, () -> service.getTimeline(10L, null));
        verifyNoInteractions(readingRepository, eventRepository, alertRepository);
    }

    @Test
    void reportsWhenTimelineReadingsWereSampled() {
        MonitoringSession session = session(MonitoringSessionStatus.COMPLETED);
        when(sessionService.getSession(10L)).thenReturn(session);
        when(readingRepository.findSampledForTimeline(eq(1L), isNull(), any(), any(), eq(200)))
                .thenReturn(List.of());
        when(readingRepository.countForTimeline(eq(1L), isNull(), any(), any())).thenReturn(201L);
        when(eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(10L)).thenReturn(List.of());
        when(alertRepository.findOverlappingRoomPeriod(eq(1L), any(), any())).thenReturn(List.of());

        var timeline = service.getTimeline(10L, null);

        assertTrue(timeline.readingsTruncated());
    }

    private MonitoringSession session(MonitoringSessionStatus status) {
        Room room = new Room(new Lab(new Organization("Org", null), "Lab", null, null),
                "Server room", RoomType.SERVER_ROOM, null, null);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = new User("user@example.com", "hash", "Test", "User", null);
        ReflectionTestUtils.setField(user, "id", 2L);
        MonitoringSession session = new MonitoringSession(room, "Cooling test", null, user);
        ReflectionTestUtils.setField(session, "id", 10L);
        ReflectionTestUtils.setField(session, "status", status);
        if (status != MonitoringSessionStatus.PLANNED) {
            ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.of(2026, 8, 25, 9, 0));
            ReflectionTestUtils.setField(session, "endedAt", LocalDateTime.of(2026, 8, 25, 10, 0));
        }
        return session;
    }
}
