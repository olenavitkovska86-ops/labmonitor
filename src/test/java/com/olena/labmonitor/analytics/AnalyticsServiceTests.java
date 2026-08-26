package com.olena.labmonitor.analytics;

import com.olena.labmonitor.alert.Alert;
import com.olena.labmonitor.alert.AlertRepository;
import com.olena.labmonitor.alert.AlertSeverity;
import com.olena.labmonitor.alert.AlertStatus;
import com.olena.labmonitor.alert.AlertType;
import com.olena.labmonitor.analytics.dto.OrganizationOverviewResponse;
import com.olena.labmonitor.analytics.dto.ProblemRoomResponse;
import com.olena.labmonitor.analytics.dto.OrganizationHistoryResponse;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 12, 0);

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private AlertRepository alertRepository;

    private AnalyticsService analyticsService;
    private Organization organization;
    private Room firstRoom;
    private Room secondRoom;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T12:00:00Z"), ZoneOffset.UTC);
        analyticsService = new AnalyticsService(
                organizationRepository,
                roomRepository,
                sensorRepository,
                alertRepository,
                clock
        );

        organization = new Organization("BioLab", null);
        ReflectionTestUtils.setField(organization, "id", 1L);
        Lab lab = new Lab(organization, "Main lab", null, null);
        ReflectionTestUtils.setField(lab, "id", 10L);
        firstRoom = room(lab, 100L, "Server room");
        secondRoom = room(lab, 200L, "Storage");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
    }

    @Test
    void buildsOrganizationOverviewFromCurrentOperationalData() {
        Alert critical = alert(1L, firstRoom, AlertSeverity.CRITICAL, AlertStatus.ACTIVE, NOW.minusMinutes(40));
        Alert acknowledged = alert(2L, firstRoom, AlertSeverity.HIGH, AlertStatus.ACKNOWLEDGED, NOW.minusHours(2));
        Alert medium = alert(3L, secondRoom, AlertSeverity.MEDIUM, AlertStatus.ACTIVE, NOW.minusMinutes(10));
        when(alertRepository.findByOrganizationIdAndStatusIn(1L, List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)))
                .thenReturn(List.of(acknowledged, critical, medium));
        when(roomRepository.countByLabOrganizationId(1L)).thenReturn(8L);
        when(sensorRepository.countByRoomLabOrganizationIdAndActiveTrueAndStatus(1L, SensorStatus.OFFLINE))
                .thenReturn(3L);

        OrganizationOverviewResponse result = analyticsService.getOrganizationOverview(1L);

        assertEquals(8, result.totalRooms());
        assertEquals(2, result.roomsRequiringAttention());
        assertEquals(3, result.unresolvedAlerts());
        assertEquals(2, result.unacknowledgedAlerts());
        assertEquals(1, result.criticalAlerts());
        assertEquals(3, result.offlineSensors());
        assertEquals(NOW, result.generatedAt());
    }

    @Test
    void ranksRoomsAndExplainsTheMainProblem() {
        Alert oldHigh = alert(1L, firstRoom, AlertSeverity.HIGH, AlertStatus.ACKNOWLEDGED, NOW.minusHours(2));
        Alert activeHigh = alert(2L, firstRoom, AlertSeverity.HIGH, AlertStatus.ACTIVE, NOW.minusMinutes(30));
        Alert critical = alert(3L, secondRoom, AlertSeverity.CRITICAL, AlertStatus.ACKNOWLEDGED, NOW.minusMinutes(15));
        when(alertRepository.findByOrganizationIdAndStatusIn(1L, List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)))
                .thenReturn(List.of(oldHigh, activeHigh, critical));

        List<ProblemRoomResponse> result = analyticsService.getProblemRooms(1L);

        assertEquals(List.of(200L, 100L), result.stream().map(ProblemRoomResponse::roomId).toList());
        ProblemRoomResponse serverRoom = result.get(1);
        assertEquals(AttentionLevel.HIGH, serverRoom.attentionLevel());
        assertEquals(2, serverRoom.unresolvedAlerts());
        assertEquals(1, serverRoom.unacknowledgedAlerts());
        assertEquals(2L, serverRoom.mainAlertId());
        assertEquals(120, serverRoom.openMinutes());
    }

    @Test
    void summarizesAlertHistoryForTheSelectedPeriod() {
        Alert resolved = alert(1L, firstRoom, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, NOW.minusDays(2));
        ReflectionTestUtils.setField(resolved, "acknowledgedAt", NOW.minusDays(2).plusMinutes(20));
        ReflectionTestUtils.setField(resolved, "resolvedAt", NOW.minusDays(2).plusMinutes(80));
        Alert active = alert(2L, firstRoom, AlertSeverity.HIGH, AlertStatus.ACTIVE, NOW.minusDays(1));
        Alert acknowledged = alert(3L, secondRoom, AlertSeverity.MEDIUM, AlertStatus.ACKNOWLEDGED, NOW.minusHours(2));
        ReflectionTestUtils.setField(acknowledged, "acknowledgedAt", NOW.minusHours(1));
        when(alertRepository.findByOrganizationIdAndCreatedAtBetween(
                1L,
                LocalDateTime.of(2026, 8, 14, 0, 0),
                NOW
        )).thenReturn(List.of(resolved, active, acknowledged));

        OrganizationHistoryResponse result = analyticsService.getOrganizationHistory(1L, AnalyticsPeriod.LAST_7_DAYS);

        assertEquals(3, result.alertsCreated());
        assertEquals(1, result.criticalAlerts());
        assertEquals(1, result.resolvedAlerts());
        assertEquals(40, result.averageAcknowledgementMinutes());
        assertEquals(80, result.averageResolutionMinutes());
        assertEquals(7, result.dailyAlerts().size());
        assertEquals(100L, result.mostProblematicRooms().getFirst().roomId());
        assertEquals(2, result.mostProblematicRooms().getFirst().alerts());
    }

    @Test
    void scopedHistoryDoesNotIncludeAlertsFromUnassignedRooms() {
        Alert allowed = alert(1L, firstRoom, AlertSeverity.HIGH, AlertStatus.ACTIVE, NOW.minusDays(1));
        Alert hidden = alert(2L, secondRoom, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, NOW.minusDays(1));
        when(alertRepository.findByOrganizationIdAndCreatedAtBetween(
                1L, LocalDateTime.of(2026, 8, 14, 0, 0), NOW
        )).thenReturn(List.of(allowed, hidden));

        OrganizationHistoryResponse result = analyticsService.getOrganizationHistory(
                1L, AnalyticsPeriod.LAST_7_DAYS, Set.of(100L));

        assertEquals(1, result.alertsCreated());
        assertEquals(0, result.criticalAlerts());
        assertEquals(List.of(100L), result.mostProblematicRooms().stream()
                .map(room -> room.roomId()).toList());
    }

    private Room room(Lab lab, Long id, String name) {
        Room room = new Room(lab, name, RoomType.EXPERIMENT_ROOM, null, null);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private Alert alert(
            Long id,
            Room room,
            AlertSeverity severity,
            AlertStatus status,
            LocalDateTime createdAt
    ) {
        Alert alert = new Alert(room, null, AlertType.SENSOR_THRESHOLD, severity, "Problem " + id, "Details");
        ReflectionTestUtils.setField(alert, "id", id);
        ReflectionTestUtils.setField(alert, "status", status);
        ReflectionTestUtils.setField(alert, "createdAt", createdAt);
        return alert;
    }
}
