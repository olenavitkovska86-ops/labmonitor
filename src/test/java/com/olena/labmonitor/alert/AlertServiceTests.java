package com.olena.labmonitor.alert;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import com.olena.labmonitor.alert.dto.ResolveAlertRequest;
import com.olena.labmonitor.alert.dto.ReopenAlertRequest;
import com.olena.labmonitor.alert.history.AlertHistory;
import com.olena.labmonitor.alert.history.AlertHistoryAction;
import com.olena.labmonitor.alert.history.AlertHistoryRepository;
import com.olena.labmonitor.config.MonitoringProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

    private static final LocalDateTime MEASURED_AT = LocalDateTime.of(2026, 8, 20, 12, 0);

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    private AlertService alertService;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(
                alertRepository,
                userRepository,
                alertHistoryRepository,
                new MonitoringProperties()
        );
        Organization organization = new Organization("Test organization", null);
        Lab lab = new Lab(organization, "Test lab", null, null);
        Room room = new Room(lab, "Test room", RoomType.EXPERIMENT_ROOM, null, null);
        sensor = new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
        sensor.updateSafeRange(new BigDecimal("18"), new BigDecimal("25"));
    }

    @Test
    void doesNotCreateAlertForValueInsideSafeRange() {
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.empty());
        alertService.processThresholdReading(sensor, new BigDecimal("21"), MEASURED_AT);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void createsAlertForValueBelowMinimum() {
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.empty());

        alertService.processThresholdReading(sensor, new BigDecimal("17"), MEASURED_AT);

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createsAlertForValueAboveMaximum() {
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.empty());

        alertService.processThresholdReading(sensor, new BigDecimal("26"), MEASURED_AT);

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void doesNotCreateDuplicateUnresolvedAlert() {
        Alert existing = thresholdAlert();
        existing.startThresholdViolation(new BigDecimal("26"), MEASURED_AT.minusMinutes(5));
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.of(existing));

        alertService.processThresholdReading(sensor, new BigDecimal("28"), MEASURED_AT);

        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals(new BigDecimal("28"), existing.getLatestValue());
        assertEquals(new BigDecimal("28"), existing.getMostExtremeValue());
        assertEquals(AlertSeverity.CRITICAL, existing.getSeverity());
    }

    @Test
    void marksExistingViolationAsRecoveredWhenValueReturnsToSafeRange() {
        Alert existing = thresholdAlert();
        existing.startThresholdViolation(new BigDecimal("28"), MEASURED_AT.minusMinutes(5));
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.of(existing));

        alertService.processThresholdReading(sensor, new BigDecimal("22"), MEASURED_AT);

        assertEquals(MEASURED_AT, existing.getRecoveredAt());
        assertEquals(new BigDecimal("28"), existing.getLatestValue());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void createsNewAlertWhenSensorBecomesUnsafeAfterRecovery() {
        Alert recovered = thresholdAlert();
        recovered.startThresholdViolation(new BigDecimal("28"), MEASURED_AT.minusMinutes(10));
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(
                nullable(Long.class), any(), anyCollection()
        )).thenReturn(Optional.of(recovered), Optional.empty());

        alertService.processThresholdReading(sensor, new BigDecimal("22"), MEASURED_AT.minusMinutes(5));
        alertService.processThresholdReading(sensor, new BigDecimal("29"), MEASURED_AT);

        assertEquals(MEASURED_AT.minusMinutes(5), recovered.getRecoveredAt());
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(new BigDecimal("29"), alertCaptor.getValue().getInitialValue());
        assertEquals(MEASURED_AT, alertCaptor.getValue().getViolationStartedAt());
    }

    @Test
    void assignsLowSeverityForDeviationUpToFivePercent() {
        assertSeverity("25.35", AlertSeverity.LOW);
    }

    @Test
    void assignsMediumSeverityForDeviationUpToFifteenPercent() {
        assertSeverity("25.70", AlertSeverity.MEDIUM);
    }

    @Test
    void assignsHighSeverityForDeviationUpToThirtyPercent() {
        assertSeverity("26.40", AlertSeverity.HIGH);
    }

    @Test
    void assignsCriticalSeverityForDeviationAboveThirtyPercent() {
        assertSeverity("29.993", AlertSeverity.CRITICAL);
    }

    @Test
    void usesHighSeverityWhenOnlyOneSafeBoundaryIsConfigured() {
        sensor.updateSafeRange(null, new BigDecimal("25"));

        assertSeverity("26", AlertSeverity.HIGH);
    }

    @Test
    void acknowledgesActiveAlert() {
        Alert alert = thresholdAlert();
        User user = authenticatedUser();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.saveAndFlush(alert)).thenReturn(alert);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        alertService.acknowledge(1L, user.getEmail());

        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
        assertNotNull(alert.getAcknowledgedAt());
        assertEquals(42L, alert.getAcknowledgedByUser().getId());
    }

    @Test
    void resolvesAcknowledgedAlert() {
        Alert alert = thresholdAlert();
        User user = authenticatedUser();
        alert.acknowledge(user);
        alert.startThresholdViolation(new BigDecimal("28"), MEASURED_AT.minusMinutes(10));
        alert.markRecovered(MEASURED_AT);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.saveAndFlush(alert)).thenReturn(alert);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        alertService.resolve(1L, user.getEmail(), new ResolveAlertRequest(
                AlertResolutionOutcome.FIXED,
                "Cooling restored"
        ));

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertNotNull(alert.getResolvedAt());
        assertEquals(42L, alert.getResolvedByUser().getId());
        assertEquals(AlertResolutionOutcome.FIXED, alert.getResolutionOutcome());
        assertEquals("Cooling restored", alert.getResolutionComment());
    }

    @Test
    void rejectsAcknowledgingResolvedAlert() {
        Alert alert = thresholdAlert();
        alert.resolve(authenticatedUser(), AlertResolutionOutcome.FALSE_ALARM, null);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.acknowledge(1L, "user@example.com"));
    }

    @Test
    void rejectsResolvingActiveAlertBeforeAcknowledgement() {
        Alert alert = thresholdAlert();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.resolve(
                1L,
                "user@example.com",
                new ResolveAlertRequest(AlertResolutionOutcome.FIXED, null)
        ));
    }

    @Test
    void rejectsFixedOutcomeWhileViolationIsOngoing() {
        Alert alert = thresholdAlert();
        alert.acknowledge(authenticatedUser());
        alert.startThresholdViolation(new BigDecimal("28"), MEASURED_AT);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.resolve(
                1L,
                "user@example.com",
                new ResolveAlertRequest(AlertResolutionOutcome.FIXED, "Cooling restarted")
        ));
    }

    @Test
    void rejectsFalseAlarmWithoutExplanation() {
        Alert alert = thresholdAlert();
        alert.acknowledge(authenticatedUser());
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.resolve(
                1L,
                "user@example.com",
                new ResolveAlertRequest(AlertResolutionOutcome.FALSE_ALARM, " ")
        ));
    }

    @Test
    void automaticallyResolvesRecoveredMediumAlert() {
        Alert alert = new Alert(
                sensor.getRoom(), sensor, AlertType.SENSOR_THRESHOLD, AlertSeverity.MEDIUM, "Threshold", "Test"
        );
        alert.startThresholdViolation(new BigDecimal("25.7"), MEASURED_AT.minusMinutes(3));
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(
                nullable(Long.class), any(), anyCollection()
        )).thenReturn(Optional.of(alert));

        alertService.processThresholdReading(sensor, new BigDecimal("22"), MEASURED_AT);

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertEquals(AlertResolutionOutcome.AUTO_RECOVERED, alert.getResolutionOutcome());
        assertEquals(MEASURED_AT, alert.getRecoveredAt());
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    void countsActiveAndAcknowledgedAlertsAsUnresolved() {
        when(alertRepository.countByStatusIn(List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)))
                .thenReturn(7L);

        assertEquals(7, alertService.countUnresolved().unresolvedAlerts());
    }

    @Test
    void reopensResolvedAlertAndClearsItsResolution() {
        Alert alert = thresholdAlert();
        User resolver = authenticatedUser();
        alert.resolve(resolver, AlertResolutionOutcome.FALSE_ALARM, "Incorrect result");
        User reopeningUser = new User("admin@example.com", "password-hash", "Lab", "Admin", null);
        ReflectionTestUtils.setField(reopeningUser, "id", 43L);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmail(reopeningUser.getEmail())).thenReturn(Optional.of(reopeningUser));
        when(alertRepository.saveAndFlush(alert)).thenReturn(alert);

        alertService.reopen(1L, reopeningUser.getEmail(), new ReopenAlertRequest("Temperature is rising again"));

        assertEquals(AlertStatus.ACTIVE, alert.getStatus());
        assertNull(alert.getResolvedAt());
        assertNull(alert.getResolutionOutcome());
        assertNull(alert.getResolutionComment());
        assertEquals(43L, alert.getReopenedByUser().getId());
        assertNotNull(alert.getReopenedAt());
        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals(AlertHistoryAction.REOPENED, historyCaptor.getValue().getAction());
        assertEquals(AlertResolutionOutcome.FALSE_ALARM, historyCaptor.getValue().getResolutionOutcome());
        assertEquals("Temperature is rising again", historyCaptor.getValue().getComment());
    }

    @Test
    void rejectsReopeningActiveAlert() {
        Alert alert = thresholdAlert();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.reopen(
                1L, "user@example.com", new ReopenAlertRequest("Incorrect resolution")
        ));
    }

    private void assertSeverity(String value, AlertSeverity expectedSeverity) {
        when(alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(nullable(Long.class), any(), anyCollection()))
                .thenReturn(Optional.empty());

        alertService.processThresholdReading(sensor, new BigDecimal(value), MEASURED_AT);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(expectedSeverity, alertCaptor.getValue().getSeverity());
    }

    private Alert thresholdAlert() {
        return new Alert(
                sensor.getRoom(),
                sensor,
                AlertType.SENSOR_THRESHOLD,
                AlertSeverity.HIGH,
                "Threshold exceeded",
                "Test alert"
        );
    }

    private User authenticatedUser() {
        User user = new User("user@example.com", "password-hash", "Test", "User", null);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }
}
