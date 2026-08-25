package com.olena.labmonitor.session.export;

import com.olena.labmonitor.alert.Alert;
import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionService;
import com.olena.labmonitor.session.MonitoringSessionStatus;
import com.olena.labmonitor.session.event.SessionEvent;
import com.olena.labmonitor.session.event.SessionEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional(readOnly = true)
public class SessionExportService {
    private static final Duration CONTEXT = Duration.ofMinutes(15);

    private final MonitoringSessionService sessionService;
    private final SensorReadingRepository readingRepository;
    private final SessionEventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final MonitoringProperties properties;

    public SessionExportService(MonitoringSessionService sessionService,
                                SensorReadingRepository readingRepository,
                                SessionEventRepository eventRepository,
                                AlertRepository alertRepository,
                                MonitoringProperties properties) {
        this.sessionService = sessionService;
        this.readingRepository = readingRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.properties = properties;
    }

    public ZipExport export(Long sessionId) {
        MonitoringSession session = sessionService.getSession(sessionId);
        if (session.getStatus() == MonitoringSessionStatus.PLANNED || session.getStartedAt() == null) {
            throw new InvalidOperationException("A monitoring session must be started before it can be exported");
        }

        LocalDateTime sessionEnd = session.getEndedAt() == null ? LocalDateTime.now() : session.getEndedAt();
        LocalDateTime exportFrom = session.getStartedAt().minus(CONTEXT);
        LocalDateTime exportTo = sessionEnd.plus(CONTEXT);
        if (Duration.between(exportFrom, exportTo).compareTo(properties.getExports().getMaxPeriod()) > 0) {
            throw new IllegalArgumentException("Session export period cannot exceed "
                    + properties.getExports().getMaxPeriod().toDays() + " days");
        }

        int maxRows = properties.getExports().getMaxRows();
        List<SensorReading> readings = readingRepository.findForExport(session.getRoom().getId(), null,
                exportFrom, exportTo, PageRequest.of(0, maxRows + 1));
        if (readings.size() > maxRows) {
            throw new IllegalArgumentException("Session export contains more than " + maxRows
                    + " readings; reduce the session duration");
        }
        List<SessionEvent> events = eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId);
        List<Alert> alerts = alertRepository.findOverlappingRoomPeriod(session.getRoom().getId(), exportFrom, exportTo);

        return new ZipExport("session-" + sessionId + "-export.zip",
                createZip(session, sessionEnd, exportFrom, exportTo, readings, events, alerts));
    }

    private byte[] createZip(MonitoringSession session, LocalDateTime sessionEnd,
                             LocalDateTime exportFrom, LocalDateTime exportTo,
                             List<SensorReading> readings, List<SessionEvent> events, List<Alert> alerts) {
        try (var output = new ByteArrayOutputStream(); var zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addEntry(zip, "session.csv", sessionCsv(session, exportFrom, exportTo));
            addEntry(zip, "readings.csv", readingsCsv(session, sessionEnd, readings));
            addEntry(zip, "events.csv", eventsCsv(session, events));
            addEntry(zip, "alerts.csv", alertsCsv(session, sessionEnd, alerts));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create session export", exception);
        }
    }

    private String sessionCsv(MonitoringSession session, LocalDateTime exportFrom, LocalDateTime exportTo) {
        Room room = session.getRoom();
        Lab lab = room.getLab();
        Organization organization = lab.getOrganization();
        var csv = csv("session_id,session_name,description,status,organization_id,organization_name,lab_id,lab_name,"
                + "room_id,room_name,started_at,ended_at,created_at,created_by_user_id,created_by_name,"
                + "export_from,export_to,context_minutes\n");
        row(csv, session.getId(), session.getName(), session.getDescription(), session.getStatus(),
                organization.getId(), organization.getName(), lab.getId(), lab.getName(), room.getId(), room.getName(),
                session.getStartedAt(), session.getEndedAt(), session.getCreatedAt(), userId(session.getCreatedBy()),
                userName(session.getCreatedBy()), exportFrom, exportTo, CONTEXT.toMinutes());
        return csv.toString();
    }

    private String readingsCsv(MonitoringSession session, LocalDateTime sessionEnd, List<SensorReading> readings) {
        Lab lab = session.getRoom().getLab();
        Organization organization = lab.getOrganization();
        var csv = csv("context_session_id,reading_id,session_phase,organization_id,organization_name,lab_id,lab_name,"
                + "room_id,room_name,sensor_id,sensor_name,sensor_type,measured_at,received_at,value,unit,"
                + "safe_min,safe_max,reading_status\n");
        for (SensorReading reading : readings) {
            Sensor sensor = reading.getSensor();
            row(csv, session.getId(), reading.getId(), phase(reading.getMeasuredAt(), session, sessionEnd),
                    organization.getId(), organization.getName(), lab.getId(), lab.getName(), reading.getRoom().getId(),
                    reading.getRoom().getName(), sensor.getId(), sensor.getName(), sensor.getType(), reading.getMeasuredAt(),
                    reading.getCreatedAt(), reading.getValue(), sensor.getUnit(), reading.getSafeMin(),
                    reading.getSafeMax(), reading.getStatus());
        }
        return csv.toString();
    }

    private String eventsCsv(MonitoringSession session, List<SessionEvent> events) {
        var csv = csv("session_id,event_id,occurred_at,created_at,category,title,description,created_by_user_id,"
                + "created_by_name\n");
        events.forEach(event -> row(csv, session.getId(), event.getId(), event.getOccurredAt(), event.getCreatedAt(),
                event.getCategory(), event.getTitle(), event.getDescription(), userId(event.getCreatedBy()),
                userName(event.getCreatedBy())));
        return csv.toString();
    }

    private String alertsCsv(MonitoringSession session, LocalDateTime sessionEnd, List<Alert> alerts) {
        Room room = session.getRoom();
        Lab lab = room.getLab();
        Organization organization = lab.getOrganization();
        var csv = csv("context_session_id,alert_id,organization_id,organization_name,lab_id,lab_name,room_id,room_name,"
                + "sensor_id,sensor_name,sensor_type,unit,alert_type,severity,workflow_status,condition_status,"
                + "condition_started_at,condition_ended_at,started_phase,ended_phase,overlaps_session,title,message,"
                + "created_at,acknowledged_at,resolved_at,reopened_at,resolution_outcome,resolution_comment,"
                + "initial_value,latest_value,most_extreme_value,last_violation_at,recovered_at\n");
        for (Alert alert : alerts) {
            Sensor sensor = alert.getSensor();
            LocalDateTime conditionStart = conditionStart(alert);
            LocalDateTime conditionEnd = conditionEnd(alert);
            row(csv, session.getId(), alert.getId(), organization.getId(), organization.getName(), lab.getId(),
                    lab.getName(), room.getId(), room.getName(), sensor == null ? null : sensor.getId(),
                    sensor == null ? null : sensor.getName(), sensor == null ? null : sensor.getType(),
                    sensor == null ? null : sensor.getUnit(), alert.getType(), alert.getSeverity(), alert.getStatus(),
                    conditionStatus(alert, conditionEnd), conditionStart, conditionEnd,
                    phase(conditionStart, session, sessionEnd), phase(conditionEnd, session, sessionEnd),
                    overlapsSession(conditionStart, conditionEnd, session.getStartedAt(), sessionEnd), alert.getTitle(),
                    alert.getMessage(), alert.getCreatedAt(), alert.getAcknowledgedAt(), alert.getResolvedAt(),
                    alert.getReopenedAt(), alert.getResolutionOutcome(), alert.getResolutionComment(),
                    alert.getInitialValue(), alert.getLatestValue(), alert.getMostExtremeValue(),
                    alert.getLastViolationAt(), alert.getRecoveredAt());
        }
        return csv.toString();
    }

    private LocalDateTime conditionStart(Alert alert) {
        return alert.getViolationStartedAt() == null ? alert.getCreatedAt() : alert.getViolationStartedAt();
    }

    private LocalDateTime conditionEnd(Alert alert) {
        return alert.getRecoveredAt() == null ? alert.getResolvedAt() : alert.getRecoveredAt();
    }

    private String conditionStatus(Alert alert, LocalDateTime conditionEnd) {
        if (conditionEnd == null) return "ONGOING";
        return alert.getRecoveredAt() == null ? "ENDED" : "RECOVERED";
    }

    private boolean overlapsSession(LocalDateTime conditionStart, LocalDateTime conditionEnd,
                                    LocalDateTime sessionStart, LocalDateTime sessionEnd) {
        return conditionStart != null && !conditionStart.isAfter(sessionEnd)
                && (conditionEnd == null || !conditionEnd.isBefore(sessionStart));
    }

    private String phase(LocalDateTime measuredAt, MonitoringSession session, LocalDateTime sessionEnd) {
        if (measuredAt == null) return null;
        if (measuredAt.isBefore(session.getStartedAt())) return "BEFORE";
        if (measuredAt.isAfter(sessionEnd)) return "AFTER";
        return "DURING";
    }

    private String userName(com.olena.labmonitor.user.User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }

    private Long userId(com.olena.labmonitor.user.User user) {
        return user == null ? null : user.getId();
    }

    private StringBuilder csv(String header) {
        return new StringBuilder("\uFEFF").append(header);
    }

    private void row(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            String text = values[index] == null ? "" : values[index].toString();
            if (!(values[index] instanceof Number) && !text.isEmpty()
                    && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
            csv.append('"').append(text.replace("\"", "\"\"")).append('"');
            csv.append(index == values.length - 1 ? '\n' : ',');
        }
    }

    private void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    public record ZipExport(String filename, byte[] content) {
    }
}
