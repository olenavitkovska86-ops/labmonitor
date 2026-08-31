package com.olena.labmonitor.analytics;

import com.olena.labmonitor.alert.Alert;
import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.alert.AlertSeverity;
import com.olena.labmonitor.alert.AlertStatus;
import com.olena.labmonitor.analytics.dto.OrganizationOverviewResponse;
import com.olena.labmonitor.analytics.dto.DailyAlertCountResponse;
import com.olena.labmonitor.analytics.dto.OrganizationHistoryResponse;
import com.olena.labmonitor.analytics.dto.ProblemRoomResponse;
import com.olena.labmonitor.analytics.dto.RoomHistoryResponse;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final List<AlertStatus> UNRESOLVED_STATUSES =
            List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final OrganizationRepository organizationRepository;
    private final RoomRepository roomRepository;
    private final SensorRepository sensorRepository;
    private final AlertRepository alertRepository;
    private final Clock clock;

    public AnalyticsService(
            OrganizationRepository organizationRepository,
            RoomRepository roomRepository,
            SensorRepository sensorRepository,
            AlertRepository alertRepository,
            Clock clock
    ) {
        this.organizationRepository = organizationRepository;
        this.roomRepository = roomRepository;
        this.sensorRepository = sensorRepository;
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    public OrganizationOverviewResponse getOrganizationOverview(Long organizationId) {
        Organization organization = getOrganization(organizationId);
        List<Alert> alerts = getUnresolvedAlerts(organizationId);

        return new OrganizationOverviewResponse(
                organization.getId(),
                organization.getName(),
                LocalDateTime.now(clock),
                roomRepository.countByLabOrganizationId(organizationId),
                alerts.stream().map(alert -> alert.getRoom().getId()).distinct().count(),
                alerts.size(),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).count(),
                alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count(),
                sensorRepository.countByRoomLabOrganizationIdAndActiveTrueAndStatus(
                        organizationId,
                        SensorStatus.OFFLINE
                )
        );
    }

    public OrganizationOverviewResponse getOrganizationOverview(Long organizationId, Set<Long> allowedRoomIds) {
        if (allowedRoomIds == null) return getOrganizationOverview(organizationId);
        Organization organization = getOrganization(organizationId);
        List<Alert> alerts = getUnresolvedAlerts(organizationId).stream()
                .filter(alert -> allowedRoomIds.contains(alert.getRoom().getId())).toList();
        long offlineSensors = sensorRepository.findAll().stream()
                .filter(sensor -> allowedRoomIds.contains(sensor.getRoom().getId()))
                .filter(sensor -> sensor.isActive() && sensor.getStatus() == SensorStatus.OFFLINE)
                .count();
        return new OrganizationOverviewResponse(
                organization.getId(), organization.getName(), LocalDateTime.now(clock), allowedRoomIds.size(),
                alerts.stream().map(alert -> alert.getRoom().getId()).distinct().count(), alerts.size(),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).count(),
                alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count(), offlineSensors);
    }

    public List<ProblemRoomResponse> getProblemRooms(Long organizationId) {
        getOrganization(organizationId);
        Map<Long, List<Alert>> alertsByRoom = new LinkedHashMap<>();
        for (Alert alert : getUnresolvedAlerts(organizationId)) {
            alertsByRoom.computeIfAbsent(alert.getRoom().getId(), ignored -> new ArrayList<>()).add(alert);
        }

        return alertsByRoom.values().stream()
                .map(this::toProblemRoom)
                .sorted(Comparator
                        .comparing(ProblemRoomResponse::attentionLevel).reversed()
                        .thenComparing(ProblemRoomResponse::unacknowledgedAlerts, Comparator.reverseOrder())
                        .thenComparing(ProblemRoomResponse::problemStartedAt))
                .toList();
    }

    public List<ProblemRoomResponse> getProblemRooms(Long organizationId, Set<Long> allowedRoomIds) {
        if (allowedRoomIds == null) return getProblemRooms(organizationId);
        return getProblemRooms(organizationId).stream()
                .filter(room -> allowedRoomIds.contains(room.roomId()))
                .toList();
    }

    public OrganizationHistoryResponse getOrganizationHistory(Long organizationId, AnalyticsPeriod period) {
        getOrganization(organizationId);
        LocalDateTime to = LocalDateTime.now(clock);
        LocalDateTime from = period == AnalyticsPeriod.LAST_24_HOURS
                ? to.minusHours(24)
                : to.toLocalDate().atStartOfDay().minusDays(period.days() - 1L);
        List<Alert> alerts = alertRepository.findByOrganizationIdAndCreatedAtBetween(organizationId, from, to);

        return new OrganizationHistoryResponse(
                organizationId,
                period,
                from,
                to,
                alerts.size(),
                alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count(),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.RESOLVED).count(),
                averageMinutes(alerts, true),
                averageMinutes(alerts, false),
                dailyCounts(alerts, from.toLocalDate(), to.toLocalDate()),
                roomHistory(alerts)
        );
    }

    public OrganizationHistoryResponse getOrganizationHistory(
            Long organizationId, AnalyticsPeriod period, Set<Long> allowedRoomIds) {
        if (allowedRoomIds == null) return getOrganizationHistory(organizationId, period);
        getOrganization(organizationId);
        LocalDateTime to = LocalDateTime.now(clock);
        LocalDateTime from = period == AnalyticsPeriod.LAST_24_HOURS
                ? to.minusHours(24)
                : to.toLocalDate().atStartOfDay().minusDays(period.days() - 1L);
        List<Alert> alerts = alertRepository.findByOrganizationIdAndCreatedAtBetween(organizationId, from, to)
                .stream().filter(alert -> allowedRoomIds.contains(alert.getRoom().getId())).toList();
        return new OrganizationHistoryResponse(
                organizationId, period, from, to, alerts.size(),
                alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count(),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.RESOLVED).count(),
                averageMinutes(alerts, true), averageMinutes(alerts, false),
                dailyCounts(alerts, from.toLocalDate(), to.toLocalDate()), roomHistory(alerts));
    }

    private ProblemRoomResponse toProblemRoom(List<Alert> alerts) {
        Alert oldest = alerts.stream().min(Comparator.comparing(Alert::getCreatedAt)).orElseThrow();
        Alert main = alerts.stream().max(Comparator
                .comparing((Alert alert) -> alert.getSeverity().ordinal())
                .thenComparing(alert -> alert.getStatus() == AlertStatus.ACTIVE ? 1 : 0)
                .thenComparing(Alert::getCreatedAt, Comparator.reverseOrder()))
                .orElseThrow();
        long unacknowledged = alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).count();
        long critical = alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count();

        return new ProblemRoomResponse(
                oldest.getRoom().getId(),
                oldest.getRoom().getName(),
                oldest.getRoom().getLab().getId(),
                oldest.getRoom().getLab().getName(),
                attentionLevel(main.getSeverity()),
                alerts.size(),
                unacknowledged,
                critical,
                main.getId(),
                main.getTitle(),
                main.getSeverity(),
                main.getStatus(),
                oldest.getCreatedAt(),
                Math.max(0, Duration.between(oldest.getCreatedAt(), LocalDateTime.now(clock)).toMinutes())
        );
    }

    private AttentionLevel attentionLevel(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> AttentionLevel.CRITICAL;
            case HIGH -> AttentionLevel.HIGH;
            case MEDIUM, LOW -> AttentionLevel.MEDIUM;
        };
    }

    private Long averageMinutes(List<Alert> alerts, boolean acknowledgement) {
        var durations = alerts.stream()
                .filter(alert -> (acknowledgement ? alert.getAcknowledgedAt() : alert.getResolvedAt()) != null)
                .mapToLong(alert -> Math.max(0, Duration.between(
                        alert.getCreatedAt(),
                        acknowledgement ? alert.getAcknowledgedAt() : alert.getResolvedAt()
                ).toMinutes()))
                .toArray();
        return durations.length == 0 ? null : Math.round(java.util.Arrays.stream(durations).average().orElse(0));
    }

    private List<DailyAlertCountResponse> dailyCounts(
            List<Alert> alerts,
            LocalDate firstDate,
            LocalDate lastDate
    ) {
        return firstDate.datesUntil(lastDate.plusDays(1))
                .map(date -> new DailyAlertCountResponse(
                        date,
                        alerts.stream().filter(alert -> alert.getCreatedAt().toLocalDate().equals(date)).count(),
                        alerts.stream().filter(alert -> alert.getCreatedAt().toLocalDate().equals(date))
                                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count()
                ))
                .toList();
    }

    private List<RoomHistoryResponse> roomHistory(List<Alert> alerts) {
        Map<Long, List<Alert>> alertsByRoom = alerts.stream()
                .collect(java.util.stream.Collectors.groupingBy(alert -> alert.getRoom().getId()));

        return alertsByRoom.values().stream()
                .map(roomAlerts -> {
                    Alert sample = roomAlerts.getFirst();
                    return new RoomHistoryResponse(
                            sample.getRoom().getId(),
                            sample.getRoom().getName(),
                            sample.getRoom().getLab().getId(),
                            sample.getRoom().getLab().getName(),
                            roomAlerts.size(),
                            roomAlerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count(),
                            roomAlerts.stream().filter(alert -> alert.getStatus() == AlertStatus.RESOLVED).count()
                    );
                })
                .sorted(Comparator.comparing(RoomHistoryResponse::alerts).reversed()
                        .thenComparing(RoomHistoryResponse::criticalAlerts, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    private List<Alert> getUnresolvedAlerts(Long organizationId) {
        return alertRepository.findByOrganizationIdAndStatusIn(organizationId, UNRESOLVED_STATUSES);
    }

    private Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization with id " + organizationId + " was not found"
                ));
    }
}
