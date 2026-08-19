package com.olena.labmonitor.alert;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    private AlertService alertService;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, userRepository);
        Organization organization = new Organization("Test organization", null);
        Lab lab = new Lab(organization, "Test lab", null, null);
        Room room = new Room(lab, "Test room", RoomType.EXPERIMENT_ROOM, null, null);
        sensor = new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
        sensor.updateSafeRange(new BigDecimal("18"), new BigDecimal("25"));
    }

    @Test
    void doesNotCreateAlertForValueInsideSafeRange() {
        alertService.createThresholdAlertIfRequired(sensor, new BigDecimal("21"));

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void createsAlertForValueBelowMinimum() {
        when(alertRepository.existsBySensorIdAndTypeAndStatusIn(nullable(Long.class), any(), anyCollection()))
                .thenReturn(false);

        alertService.createThresholdAlertIfRequired(sensor, new BigDecimal("17"));

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createsAlertForValueAboveMaximum() {
        when(alertRepository.existsBySensorIdAndTypeAndStatusIn(nullable(Long.class), any(), anyCollection()))
                .thenReturn(false);

        alertService.createThresholdAlertIfRequired(sensor, new BigDecimal("26"));

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void doesNotCreateDuplicateUnresolvedAlert() {
        when(alertRepository.existsBySensorIdAndTypeAndStatusIn(nullable(Long.class), any(), anyCollection()))
                .thenReturn(true);

        alertService.createThresholdAlertIfRequired(sensor, new BigDecimal("26"));

        verify(alertRepository, never()).save(any(Alert.class));
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
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.saveAndFlush(alert)).thenReturn(alert);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        alertService.resolve(1L, user.getEmail());

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertNotNull(alert.getResolvedAt());
        assertEquals(42L, alert.getResolvedByUser().getId());
    }

    @Test
    void rejectsAcknowledgingResolvedAlert() {
        Alert alert = thresholdAlert();
        alert.resolve(authenticatedUser());
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(RuntimeException.class, () -> alertService.acknowledge(1L, "user@example.com"));
    }

    private void assertSeverity(String value, AlertSeverity expectedSeverity) {
        when(alertRepository.existsBySensorIdAndTypeAndStatusIn(nullable(Long.class), any(), anyCollection()))
                .thenReturn(false);

        alertService.createThresholdAlertIfRequired(sensor, new BigDecimal(value));

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
