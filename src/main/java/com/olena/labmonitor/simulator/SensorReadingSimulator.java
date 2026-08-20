package com.olena.labmonitor.simulator;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingService;
import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "labmonitor.simulator.enabled", havingValue = "true")
public class SensorReadingSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingSimulator.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingService sensorReadingService;
    private final SensorSimulatorProperties properties;
    private final SensorValueScenario scenario = new SensorValueScenario();
    private final ConcurrentHashMap<Long, AtomicLong> sensorSteps = new ConcurrentHashMap<>();

    public SensorReadingSimulator(
            SensorRepository sensorRepository,
            SensorReadingService sensorReadingService,
            SensorSimulatorProperties properties
    ) {
        this.sensorRepository = sensorRepository;
        this.sensorReadingService = sensorReadingService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${labmonitor.simulator.interval:1m}",
            initialDelayString = "${labmonitor.simulator.startup-delay:10s}"
    )
    public void generateReadings() {
        var sensors = sensorRepository.findByActiveTrueOrderByIdAsc(PageRequest.of(0, properties.getMaxSensors()));
        LocalDateTime measuredAt = LocalDateTime.now();

        for (Sensor sensor : sensors) {
            if (sensor.getMinSafeValue() == null && sensor.getMaxSafeValue() == null) {
                continue;
            }
            long step = sensorSteps.computeIfAbsent(sensor.getId(), ignored -> new AtomicLong()).getAndIncrement();
            try {
                sensorReadingService.create(new CreateSensorReadingRequest(
                        sensor.getId(), scenario.valueFor(sensor, step), measuredAt
                ));
            } catch (InvalidOperationException exception) {
                log.debug("Simulator skipped sensor {}: {}", sensor.getId(), exception.getMessage());
            } catch (RuntimeException exception) {
                log.warn("Simulator could not generate a reading for sensor {}", sensor.getId(), exception);
            }
        }
    }
}
