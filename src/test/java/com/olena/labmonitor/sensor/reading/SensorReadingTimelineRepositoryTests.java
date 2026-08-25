package com.olena.labmonitor.sensor.reading;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SensorReadingTimelineRepositoryTests {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 25, 8, 0);

    @Autowired
    private SensorReadingRepository readingRepository;
    @Autowired
    private SensorRepository sensorRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private LabRepository labRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    private Room room;
    private Sensor frequentSensor;
    private Sensor sparseSensor;

    @BeforeEach
    void setUp() {
        Organization organization = organizationRepository.save(new Organization("Timeline organization", null));
        Lab lab = labRepository.save(new Lab(organization, "Timeline lab", null, null));
        room = roomRepository.save(new Room(lab, "Timeline room", RoomType.EXPERIMENT_ROOM, null, null));
        frequentSensor = sensorRepository.save(new Sensor(room, "Frequent", SensorType.TEMPERATURE, "C"));
        sparseSensor = sensorRepository.save(new Sensor(room, "Sparse", SensorType.HUMIDITY, "%"));
    }

    @Test
    void samplesEachSensorAcrossTheEntireTimeline() {
        saveReadings(frequentSensor, 401, 1);
        saveReadings(sparseSensor, 10, 40);
        readingRepository.flush();

        List<SensorReading> sampled = readingRepository.findSampledForTimeline(
                room.getId(), null, FROM, FROM.plusMinutes(400), 200);

        List<SensorReading> frequent = forSensor(sampled, frequentSensor.getId());
        List<SensorReading> sparse = forSensor(sampled, sparseSensor.getId());
        assertEquals(200, frequent.size());
        assertEquals(10, sparse.size());
        assertEquals(FROM, frequent.getFirst().getMeasuredAt());
        assertEquals(FROM.plusMinutes(400), frequent.getLast().getMeasuredAt());
        assertEquals(FROM, sparse.getFirst().getMeasuredAt());
        assertEquals(FROM.plusMinutes(360), sparse.getLast().getMeasuredAt());
        assertEquals(411, readingRepository.countForTimeline(
                room.getId(), null, FROM, FROM.plusMinutes(400)));
    }

    @Test
    void limitsTimelineToSelectedSensor() {
        saveReadings(frequentSensor, 3, 1);
        saveReadings(sparseSensor, 3, 1);
        readingRepository.flush();

        List<SensorReading> sampled = readingRepository.findSampledForTimeline(
                room.getId(), sparseSensor.getId(), FROM, FROM.plusMinutes(2), 200);

        assertEquals(3, sampled.size());
        assertEquals(List.of(sparseSensor.getId(), sparseSensor.getId(), sparseSensor.getId()),
                sampled.stream().map(reading -> reading.getSensor().getId()).toList());
    }

    private void saveReadings(Sensor sensor, int count, int intervalMinutes) {
        for (int index = 0; index < count; index++) {
            readingRepository.save(new SensorReading(sensor, BigDecimal.valueOf(index),
                    FROM.plusMinutes((long) index * intervalMinutes)));
        }
    }

    private List<SensorReading> forSensor(List<SensorReading> readings, Long sensorId) {
        return readings.stream().filter(reading -> reading.getSensor().getId().equals(sensorId)).toList();
    }
}
