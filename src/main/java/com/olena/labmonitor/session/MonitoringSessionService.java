package com.olena.labmonitor.session;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.session.dto.CreateMonitoringSessionRequest;
import com.olena.labmonitor.session.dto.MonitoringSessionResponse;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MonitoringSessionService {

    private final MonitoringSessionRepository repository;
    private final RoomService roomService;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public MonitoringSessionService(MonitoringSessionRepository repository, RoomService roomService,
                                    UserRepository userRepository) {
        this(repository, roomService, userRepository, Clock.systemDefaultZone());
    }

    MonitoringSessionService(MonitoringSessionRepository repository, RoomService roomService,
                             UserRepository userRepository, Clock clock) {
        this.repository = repository;
        this.roomService = roomService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public MonitoringSessionResponse create(CreateMonitoringSessionRequest request, String email) {
        Room room = roomService.getExistingRoom(request.roomId());
        requireOperationalRoom(room);
        MonitoringSession session = new MonitoringSession(room, request.name().trim(),
                trimToNull(request.description()), getUser(email));
        return MonitoringSessionResponse.from(repository.saveAndFlush(session));
    }

    @Transactional(readOnly = true)
    public List<MonitoringSessionResponse> findAll(Long roomId, MonitoringSessionStatus status) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<MonitoringSession> sessions;
        if (roomId != null && status != null) sessions = repository.findByRoomIdAndStatus(roomId, status, sort);
        else if (roomId != null) sessions = repository.findByRoomId(roomId, sort);
        else if (status != null) sessions = repository.findByStatus(status, sort);
        else sessions = repository.findAll(sort);
        return sessions.stream().map(MonitoringSessionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MonitoringSessionResponse findById(Long id) {
        return MonitoringSessionResponse.from(getSession(id));
    }

    public MonitoringSessionResponse start(Long id) {
        MonitoringSession session = getSession(id);
        if (session.getStatus() != MonitoringSessionStatus.PLANNED) {
            throw new InvalidOperationException("Only a planned monitoring session can be started");
        }
        requireOperationalRoom(session.getRoom());
        if (repository.existsByRoomIdAndStatus(session.getRoom().getId(), MonitoringSessionStatus.ACTIVE)) {
            throw new InvalidOperationException("Room " + session.getRoom().getId()
                    + " already has an active monitoring session");
        }
        session.start(LocalDateTime.now(clock));
        return MonitoringSessionResponse.from(repository.saveAndFlush(session));
    }

    public MonitoringSessionResponse complete(Long id) {
        MonitoringSession session = requireActive(id, "completed");
        session.complete(LocalDateTime.now(clock));
        return MonitoringSessionResponse.from(repository.saveAndFlush(session));
    }

    public MonitoringSessionResponse cancel(Long id) {
        MonitoringSession session = getSession(id);
        if (session.getStatus() != MonitoringSessionStatus.PLANNED
                && session.getStatus() != MonitoringSessionStatus.ACTIVE) {
            throw new InvalidOperationException("Only a planned or active monitoring session can be cancelled");
        }
        session.cancel(LocalDateTime.now(clock));
        return MonitoringSessionResponse.from(repository.saveAndFlush(session));
    }

    public MonitoringSession requireActive(Long id, String operation) {
        MonitoringSession session = getSession(id);
        if (session.getStatus() != MonitoringSessionStatus.ACTIVE) {
            throw new InvalidOperationException("Only an active monitoring session can be " + operation);
        }
        return session;
    }

    public MonitoringSession getSession(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Monitoring session with id " + id + " was not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("User was not found for authenticated account " + email));
    }

    private void requireOperationalRoom(Room room) {
        if (!room.isActive() || !room.getLab().isActive()) {
            throw new InvalidOperationException("Monitoring sessions require an active room and lab");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
