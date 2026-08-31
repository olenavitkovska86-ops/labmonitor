package com.olena.labmonitor.session.timeline;

import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.alert.dto.AlertResponse;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.dto.MonitoringSessionResponse;
import com.olena.labmonitor.session.event.SessionEventRepository;
import com.olena.labmonitor.session.event.dto.SessionEventResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SessionTimelineService {
    private static final int POINTS_PER_SENSOR = 200;

    private final MonitoringSessionService sessionService;
    private final SensorReadingRepository readingRepository;
    private final SessionEventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final Clock clock;

    public SessionTimelineService(MonitoringSessionService sessionService,
                                  SensorReadingRepository readingRepository,
                                  SessionEventRepository eventRepository,
                                  AlertRepository alertRepository,
                                  Clock clock) {
        this.sessionService = sessionService;
        this.readingRepository = readingRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    public SessionTimelineResponse getTimeline(Long sessionId, Long sensorId) {
        MonitoringSession session = sessionService.getSession(sessionId);
        if (session.getStatus() == MonitoringSessionStatus.PLANNED || session.getStartedAt() == null) {
            throw new InvalidOperationException("A monitoring session must be started before its timeline is available");
        }
        LocalDateTime from = session.getStartedAt();
        LocalDateTime to = session.getEndedAt() == null ? LocalDateTime.now(clock) : session.getEndedAt();

        List<SensorReading> sampled = readingRepository.findSampledForTimeline(
                session.getRoom().getId(), sensorId, from, to, POINTS_PER_SENSOR);
        long totalReadings = readingRepository.countForTimeline(session.getRoom().getId(), sensorId, from, to);
        boolean truncated = totalReadings > sampled.size();
        var readings = sampled.stream().map(SessionTimelineResponse.TimelineReading::from).toList();
        var events = eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId).stream()
                .map(SessionEventResponse::from).toList();
        var alerts = alertRepository.findOverlappingRoomPeriod(session.getRoom().getId(), from, to).stream()
                .map(AlertResponse::from).toList();
        return new SessionTimelineResponse(MonitoringSessionResponse.from(session), from, to,
                readings, events, alerts, truncated);
    }
}
