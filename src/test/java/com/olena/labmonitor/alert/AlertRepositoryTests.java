package com.olena.labmonitor.alert;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlertRepositoryTests {

    private static final LocalDateTime SESSION_START = LocalDateTime.of(2026, 8, 25, 10, 0);
    private static final LocalDateTime SESSION_END = SESSION_START.plusMinutes(30);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LabRepository labRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private Room room;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        Organization organization = organizationRepository.save(new Organization("Test organization", null));
        Lab lab = labRepository.save(new Lab(organization, "Test lab", null, null));
        room = roomRepository.save(new Room(lab, "Test room", RoomType.EXPERIMENT_ROOM, null, null));
        sensor = sensorRepository.save(new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C"));
    }

    @Test
    void includesAlertWhoseConditionIsEntirelyInsideSession() {
        Alert alert = thresholdAlert(SESSION_START.plusMinutes(5), SESSION_START.plusMinutes(10));

        assertEquals(java.util.List.of(alert.getId()), overlappingAlertIds());
    }

    @Test
    void includesAlertThatStartsBeforeAndRecoversDuringSession() {
        Alert alert = thresholdAlert(SESSION_START.minusMinutes(5), SESSION_START.plusMinutes(5));

        assertEquals(java.util.List.of(alert.getId()), overlappingAlertIds());
    }

    @Test
    void includesAlertThatStartsDuringAndRecoversAfterSession() {
        Alert alert = thresholdAlert(SESSION_START.plusMinutes(10), SESSION_END.plusMinutes(5));

        assertEquals(java.util.List.of(alert.getId()), overlappingAlertIds());
    }

    @Test
    void includesOngoingAlertThatStartsBeforeSession() {
        Alert alert = thresholdAlert(SESSION_START.minusMinutes(5), null);

        assertEquals(java.util.List.of(alert.getId()), overlappingAlertIds());
    }

    @Test
    void excludesRecoveredUnresolvedAlertWhoseConditionEndedBeforeSession() {
        thresholdAlert(SESSION_START.minusMinutes(10), SESSION_START.minusMinutes(1));

        assertEquals(java.util.List.of(), overlappingAlertIds());
    }

    private Alert thresholdAlert(LocalDateTime violationStartedAt, LocalDateTime recoveredAt) {
        Alert alert = new Alert(room, sensor, AlertType.SENSOR_THRESHOLD, AlertSeverity.HIGH,
                "Threshold exceeded", "Test alert");
        alert.startThresholdViolation(new BigDecimal("26"), violationStartedAt);
        if (recoveredAt != null) alert.markRecovered(recoveredAt);
        return alertRepository.saveAndFlush(alert);
    }

    private java.util.List<Long> overlappingAlertIds() {
        return alertRepository.findOverlappingRoomPeriod(room.getId(), SESSION_START, SESSION_END).stream()
                .map(Alert::getId)
                .toList();
    }
}
