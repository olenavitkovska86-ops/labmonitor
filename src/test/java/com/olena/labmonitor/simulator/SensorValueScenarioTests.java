package com.olena.labmonitor.simulator;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorValueScenarioTests {

    private final SensorValueScenario scenario = new SensorValueScenario();

    @Test
    void evenSensorProducesShortMildViolationAndRecovery() {
        Sensor sensor = sensor(2L);

        BigDecimal unsafe = scenario.valueFor(sensor, stepForPosition(sensor, 6));
        BigDecimal recovered = scenario.valueFor(sensor, stepForPosition(sensor, 8));

        assertEquals(new BigDecimal("25.210"), unsafe);
        assertTrue(recovered.compareTo(sensor.getMinSafeValue()) >= 0);
        assertTrue(recovered.compareTo(sensor.getMaxSafeValue()) <= 0);
    }

    @Test
    void oddSensorEscalatesToCriticalViolation() {
        Sensor sensor = sensor(1L);

        assertEquals(
                new BigDecimal("27.800"),
                scenario.valueFor(sensor, stepForPosition(sensor, 9))
        );
    }

    private long stepForPosition(Sensor sensor, int position) {
        return Math.floorMod(position - sensor.getId().intValue(), 12);
    }

    private Sensor sensor(Long id) {
        Organization organization = new Organization("Test", null);
        Lab lab = new Lab(organization, "Lab", null, null);
        Room room = new Room(lab, "Room", RoomType.EXPERIMENT_ROOM, null, null);
        Sensor sensor = new Sensor(room, "Temperature", SensorType.TEMPERATURE, "C");
        sensor.updateSafeRange(new BigDecimal("18"), new BigDecimal("25"));
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }
}
