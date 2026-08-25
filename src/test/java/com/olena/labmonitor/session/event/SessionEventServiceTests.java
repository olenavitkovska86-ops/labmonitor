package com.olena.labmonitor.session.event;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.event.dto.CreateSessionEventRequest;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionEventServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);
    private final SessionEventRepository repository = mock(SessionEventRepository.class);
    private final MonitoringSessionService sessionService = mock(MonitoringSessionService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SessionEventService service = new SessionEventService(repository, sessionService, userRepository, CLOCK);

    @Test
    void createsTimestampedEventForActiveSession() {
        MonitoringSession session = activeSession();
        User creator = session.getCreatedBy();
        var request = new CreateSessionEventRequest(SessionEventCategory.INTERVENTION,
                " Restarted cooling ", " Restored airflow ", LocalDateTime.of(2026, 8, 25, 9, 45));
        when(sessionService.requireActive(10L, "given new events")).thenReturn(session);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(creator));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(10L, request, "user@example.com");

        assertEquals("Restarted cooling", response.title());
        assertEquals("Restored airflow", response.description());
        assertEquals(SessionEventCategory.INTERVENTION, response.category());
    }

    @Test
    void rejectsEventBeforeSessionStart() {
        MonitoringSession session = activeSession();
        when(sessionService.requireActive(10L, "given new events")).thenReturn(session);
        var request = new CreateSessionEventRequest(SessionEventCategory.OBSERVATION,
                "Observation", null, LocalDateTime.of(2026, 8, 25, 8, 59));

        assertThrows(InvalidOperationException.class, () -> service.create(10L, request, "user@example.com"));
    }

    @Test
    void rejectsFutureEvent() {
        MonitoringSession session = activeSession();
        when(sessionService.requireActive(10L, "given new events")).thenReturn(session);
        var request = new CreateSessionEventRequest(SessionEventCategory.OTHER,
                "Future", null, LocalDateTime.of(2026, 8, 25, 10, 1));

        assertThrows(InvalidOperationException.class, () -> service.create(10L, request, "user@example.com"));
    }

    private MonitoringSession activeSession() {
        Room room = new Room(new Lab(new Organization("Org", null), "Lab", null, null),
                "Room", RoomType.SERVER_ROOM, null, null);
        User user = new User("user@example.com", "hash", "Test", "User", null);
        ReflectionTestUtils.setField(user, "id", 2L);
        MonitoringSession session = new MonitoringSession(room, "Cooling test", null, user);
        ReflectionTestUtils.setField(session, "id", 10L);
        ReflectionTestUtils.setField(session, "status", MonitoringSessionStatus.ACTIVE);
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.of(2026, 8, 25, 9, 0));
        return session;
    }
}
