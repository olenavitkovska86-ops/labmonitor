package com.olena.labmonitor.session;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringSessionServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    private final MonitoringSessionRepository repository = mock(MonitoringSessionRepository.class);
    private final RoomService roomService = mock(RoomService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MonitoringSessionService service = new MonitoringSessionService(repository, roomService, userRepository, CLOCK);

    @Test
    void startsPlannedSessionWhenRoomHasNoActiveSession() {
        MonitoringSession session = session(MonitoringSessionStatus.PLANNED);
        when(repository.findById(10L)).thenReturn(Optional.of(session));
        when(repository.existsByRoomIdAndStatus(1L, MonitoringSessionStatus.ACTIVE)).thenReturn(false);
        when(repository.saveAndFlush(session)).thenReturn(session);

        var response = service.start(10L);

        assertEquals(MonitoringSessionStatus.ACTIVE, response.status());
        assertEquals("2026-08-25T10:00", response.startedAt().toString());
    }

    @Test
    void rejectsSecondActiveSessionInSameRoom() {
        MonitoringSession session = session(MonitoringSessionStatus.PLANNED);
        when(repository.findById(10L)).thenReturn(Optional.of(session));
        when(repository.existsByRoomIdAndStatus(1L, MonitoringSessionStatus.ACTIVE)).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> service.start(10L));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void completesOnlyActiveSession() {
        MonitoringSession session = session(MonitoringSessionStatus.PLANNED);
        when(repository.findById(10L)).thenReturn(Optional.of(session));

        assertThrows(InvalidOperationException.class, () -> service.complete(10L));
    }

    private MonitoringSession session(MonitoringSessionStatus status) {
        Organization organization = new Organization("Organization", null);
        Lab lab = new Lab(organization, "Lab", null, null);
        Room room = new Room(lab, "Room", RoomType.SERVER_ROOM, null, null);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = new User("user@example.com", "hash", "Test", "User", null);
        ReflectionTestUtils.setField(user, "id", 2L);
        MonitoringSession session = new MonitoringSession(room, "Cooling test", null, user);
        ReflectionTestUtils.setField(session, "id", 10L);
        ReflectionTestUtils.setField(session, "status", status);
        return session;
    }
}
