package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.alert.AlertService;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.device.Device;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorService;
import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import com.olena.labmonitor.config.MonitoringProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Service
@Transactional
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final SensorService sensorService;
    private final AlertService alertService;
    private final MonitoringProperties monitoringProperties;

    public SensorReadingService(
            SensorReadingRepository sensorReadingRepository,
            SensorService sensorService,
            AlertService alertService,
            MonitoringProperties monitoringProperties
    ) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.sensorService = sensorService;
        this.alertService = alertService;
        this.monitoringProperties = monitoringProperties;
    }

    public SensorReadingResponse create(CreateSensorReadingRequest request) {
        Sensor sensor = sensorService.getExistingSensor(request.sensorId());
        if (!sensor.isActive()) {
            throw new InvalidOperationException(
                    "Inactive sensor with id " + sensor.getId() + " cannot accept readings"
            );
        }
        sensorService.requireOperationalParents(sensor, "accept a reading for sensor with id " + sensor.getId());

        LocalDateTime measuredAt = request.measuredAt() == null
                ? LocalDateTime.now()
                : request.measuredAt();

        return createForSensor(sensor, request.value(), measuredAt, null, null);
    }

    public SensorReadingResponse createFromDevice(
            Sensor sensor,
            java.math.BigDecimal value,
            LocalDateTime measuredAt,
            Device device,
            String messageId
    ) {
        if (!sensor.isActive()) {
            throw new InvalidOperationException(
                    "Inactive sensor with id " + sensor.getId() + " cannot accept readings"
            );
        }
        sensorService.requireOperationalParents(sensor, "accept a reading for sensor with id " + sensor.getId());
        return createForSensor(sensor, value, measuredAt, device, messageId);
    }

    private SensorReadingResponse createForSensor(
            Sensor sensor,
            java.math.BigDecimal value,
            LocalDateTime measuredAt,
            Device device,
            String messageId
    ) {
        SensorReading reading = new SensorReading(sensor, value, measuredAt, device, messageId);
        SensorReading savedReading = sensorReadingRepository.saveAndFlush(reading);
        LocalDateTime receivedAt = savedReading.getCreatedAt();
        // Every received packet proves liveness, but historical measurements must not rewrite current threshold state.
        sensor.recordReading(receivedAt);
        alertService.processSensorOnline(sensor, receivedAt);
        if (isCurrentReading(savedReading)) {
            alertService.processThresholdReading(sensor, savedReading.getValue(), savedReading.getMeasuredAt());
        }

        return SensorReadingResponse.from(savedReading);
    }

    private boolean isCurrentReading(SensorReading savedReading) {
        if (savedReading.getId() == null) return false;
        return sensorReadingRepository
                .findFirstBySensorIdOrderByMeasuredAtDescIdDesc(savedReading.getSensor().getId())
                .map(current -> savedReading.getId().equals(current.getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<SensorReadingResponse> findCurrent(Long sensorId) {
        sensorService.getExistingSensor(sensorId);

        return sensorReadingRepository.findFirstBySensorIdOrderByMeasuredAtDescIdDesc(sensorId)
                .map(SensorReadingResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> findHistory(
            Long sensorId,
            LocalDateTime from,
            LocalDateTime to,
            Integer limit
    ) {
        sensorService.getExistingSensor(sensorId);

        int maximumLimit = monitoringProperties.getReadings().getHistoryMaxResults();
        int effectiveLimit = limit == null ? maximumLimit : limit;
        if (effectiveLimit < 1 || effectiveLimit > maximumLimit) {
            throw new IllegalArgumentException("History limit must be between 1 and " + maximumLimit);
        }

        LocalDateTime effectiveTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime effectiveFrom = from == null
                ? effectiveTo.minus(monitoringProperties.getReadings().getHistoryDefaultPeriod())
                : from;
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new IllegalArgumentException("History start time must be before end time");
        }

        return sensorReadingRepository
                .findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDescIdDesc(
                        sensorId,
                        effectiveFrom,
                        effectiveTo,
                        PageRequest.of(0, effectiveLimit)
                )
                .stream()
                .map(SensorReadingResponse::from)
                .toList();
    }
}
