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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SessionTimelineService {
    private static final int QUERY_LIMIT = 5_000;
    private static final int POINTS_PER_SENSOR = 200;

    private final MonitoringSessionService sessionService;
    private final SensorReadingRepository readingRepository;
    private final SessionEventRepository eventRepository;
    private final AlertRepository alertRepository;

    public SessionTimelineService(MonitoringSessionService sessionService,
                                  SensorReadingRepository readingRepository,
                                  SessionEventRepository eventRepository,
                                  AlertRepository alertRepository) {
        this.sessionService = sessionService;
        this.readingRepository = readingRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
    }

    public SessionTimelineResponse getTimeline(Long sessionId, Long sensorId) {
        MonitoringSession session = sessionService.getSession(sessionId);
        if (session.getStatus() == MonitoringSessionStatus.PLANNED || session.getStartedAt() == null) {
            throw new InvalidOperationException("A monitoring session must be started before its timeline is available");
        }
        LocalDateTime from = session.getStartedAt();
        LocalDateTime to = session.getEndedAt() == null ? LocalDateTime.now() : session.getEndedAt();

        List<SensorReading> newest = readingRepository.findForTimeline(
                session.getRoom().getId(), sensorId, from, to, PageRequest.of(0, QUERY_LIMIT + 1));
        boolean truncated = newest.size() > QUERY_LIMIT;
        if (truncated) newest = new ArrayList<>(newest.subList(0, QUERY_LIMIT));
        else newest = new ArrayList<>(newest);
        Collections.reverse(newest);

        var readings = downsample(newest).stream().map(SessionTimelineResponse.TimelineReading::from).toList();
        var events = eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId).stream()
                .map(SessionEventResponse::from).toList();
        var alerts = alertRepository.findOverlappingRoomPeriod(session.getRoom().getId(), from, to).stream()
                .map(AlertResponse::from).toList();
        return new SessionTimelineResponse(MonitoringSessionResponse.from(session), from, to,
                readings, events, alerts, truncated);
    }

    private List<SensorReading> downsample(List<SensorReading> readings) {
        var bySensor = new LinkedHashMap<Long, List<SensorReading>>();
        readings.forEach(reading -> bySensor.computeIfAbsent(reading.getSensor().getId(), ignored -> new ArrayList<>())
                .add(reading));
        List<SensorReading> result = new ArrayList<>();
        for (List<SensorReading> sensorReadings : bySensor.values()) {
            if (sensorReadings.size() <= POINTS_PER_SENSOR) {
                result.addAll(sensorReadings);
                continue;
            }
            for (int index = 0; index < POINTS_PER_SENSOR; index++) {
                int sourceIndex = (int) Math.round(index * (sensorReadings.size() - 1.0) / (POINTS_PER_SENSOR - 1));
                result.add(sensorReadings.get(sourceIndex));
            }
        }
        result.sort(java.util.Comparator.comparing(SensorReading::getMeasuredAt).thenComparing(SensorReading::getId));
        return result;
    }
}
