package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorService;
import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final SensorService sensorService;

    public SensorReadingService(
            SensorReadingRepository sensorReadingRepository,
            SensorService sensorService
    ) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.sensorService = sensorService;
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

        SensorReading reading = new SensorReading(sensor, request.value(), measuredAt);
        SensorReading savedReading = sensorReadingRepository.saveAndFlush(reading);
        sensor.recordReading(measuredAt);

        return SensorReadingResponse.from(savedReading);
    }

    @Transactional(readOnly = true)
    public Optional<SensorReadingResponse> findCurrent(Long sensorId) {
        sensorService.getExistingSensor(sensorId);

        return sensorReadingRepository.findFirstBySensorIdOrderByMeasuredAtDescIdDesc(sensorId)
                .map(SensorReadingResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> findHistory(Long sensorId) {
        sensorService.getExistingSensor(sensorId);

        return sensorReadingRepository.findHistoryBySensorId(sensorId).stream()
                .map(SensorReadingResponse::from)
                .toList();
    }
}
