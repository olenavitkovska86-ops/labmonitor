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

    @Test
    void humidityRisesGraduallyAndCrossesBoundaryLater() {
        Sensor sensor = sensor(2L, SensorType.HUMIDITY, "40", "60");

        assertEquals(new BigDecimal("59.000"), scenario.valueFor(sensor, stepForPosition(sensor, 6)));
        assertEquals(new BigDecimal("60.600"), scenario.valueFor(sensor, stepForPosition(sensor, 7)));
        assertEquals(new BigDecimal("65.600"), scenario.valueFor(sensor, stepForPosition(sensor, 9)));
        assertEquals(new BigDecimal("55.000"), scenario.valueFor(sensor, stepForPosition(sensor, 11)));
    }

    private long stepForPosition(Sensor sensor, int position) {
        return Math.floorMod(position - sensor.getId().intValue(), 12);
    }

    private Sensor sensor(Long id) {
        return sensor(id, SensorType.TEMPERATURE, "18", "25");
    }

    private Sensor sensor(Long id, SensorType type, String minimum, String maximum) {
        Organization organization = new Organization("Test", null);
        Lab lab = new Lab(organization, "Lab", null, null);
        Room room = new Room(lab, "Room", RoomType.EXPERIMENT_ROOM, null, null);
        Sensor sensor = new Sensor(room, type.name(), type, "unit");
        sensor.updateSafeRange(new BigDecimal(minimum), new BigDecimal(maximum));
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }
}
