package com.olena.labmonitor.session.event;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.event.dto.CreateSessionEventRequest;
import com.olena.labmonitor.session.event.dto.SessionEventResponse;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SessionEventService {
    private final SessionEventRepository repository;
    private final MonitoringSessionService sessionService;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public SessionEventService(SessionEventRepository repository, MonitoringSessionService sessionService,
                               UserRepository userRepository) {
        this(repository, sessionService, userRepository, Clock.systemDefaultZone());
    }

    SessionEventService(SessionEventRepository repository, MonitoringSessionService sessionService,
                        UserRepository userRepository, Clock clock) {
        this.repository = repository;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public SessionEventResponse create(Long sessionId, CreateSessionEventRequest request, String email) {
        MonitoringSession session = sessionService.requireActive(sessionId, "given new events");
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.occurredAt().isBefore(session.getStartedAt())) {
            throw new InvalidOperationException("Event time cannot be before the monitoring session started");
        }
        if (request.occurredAt().isAfter(now)) {
            throw new InvalidOperationException("Event time cannot be in the future");
        }
        User creator = userRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("User was not found for authenticated account " + email));
        SessionEvent event = new SessionEvent(session, request.category(), request.title().trim(),
                trimToNull(request.description()), request.occurredAt(), creator);
        return SessionEventResponse.from(repository.saveAndFlush(event));
    }

    @Transactional(readOnly = true)
    public List<SessionEventResponse> findAll(Long sessionId) {
        sessionService.getSession(sessionId);
        return repository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId).stream()
                .map(SessionEventResponse::from).toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
