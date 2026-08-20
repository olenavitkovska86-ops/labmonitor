package com.olena.labmonitor.simulator;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingService;
import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SensorReadingSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingSimulator.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingService sensorReadingService;
    private final SensorSimulatorProperties properties;
    private final SensorValueScenario scenario = new SensorValueScenario();
    private final ConcurrentHashMap<Long, AtomicLong> sensorSteps = new ConcurrentHashMap<>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private volatile long intervalSeconds = 60;
    private volatile long lastRunNanos;

    public SensorReadingSimulator(
            SensorRepository sensorRepository,
            SensorReadingService sensorReadingService,
            SensorSimulatorProperties properties
    ) {
        this.sensorRepository = sensorRepository;
        this.sensorReadingService = sensorReadingService;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void tick() {
        if (!enabled.get()) {
            return;
        }
        long now = System.nanoTime();
        if (lastRunNanos != 0 && now - lastRunNanos < intervalSeconds * 1_000_000_000L) {
            return;
        }
        lastRunNanos = now;
        generateReadings();
    }

    public SimulatorStatus start(long requestedIntervalSeconds) {
        if (requestedIntervalSeconds != 5 && requestedIntervalSeconds != 60) {
            throw new IllegalArgumentException("Simulator interval must be 5 or 60 seconds");
        }
        long eligibleSensors = sensorRepository.countSimulatorEligibleSensors();
        if (eligibleSensors == 0) {
            throw new InvalidOperationException(
                    "Create an active sensor with a configured safe range before starting the simulator"
            );
        }
        intervalSeconds = requestedIntervalSeconds;
        lastRunNanos = 0;
        enabled.set(true);
        return status();
    }

    public SimulatorStatus stop() {
        enabled.set(false);
        return status();
    }

    public SimulatorStatus status() {
        long eligible = sensorRepository.countSimulatorEligibleSensors();
        return new SimulatorStatus(enabled.get(), intervalSeconds, eligible, properties.getMaxSensors());
    }

    private void generateReadings() {
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

    public record SimulatorStatus(
            boolean enabled,
            long intervalSeconds,
            long eligibleSensors,
            int maxSensors
    ) {
    }
}
