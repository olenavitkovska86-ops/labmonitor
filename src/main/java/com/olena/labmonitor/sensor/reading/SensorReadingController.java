package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    public SensorReadingController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    @PostMapping("/api/sensor-readings")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingResponse create(@Valid @RequestBody CreateSensorReadingRequest request) {
        return sensorReadingService.create(request);
    }

    @GetMapping("/api/sensors/{sensorId}/current-reading")
    public ResponseEntity<SensorReadingResponse> findCurrent(@PathVariable Long sensorId) {
        return sensorReadingService.findCurrent(sensorId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/sensors/{sensorId}/readings")
    public List<SensorReadingResponse> findHistory(@PathVariable Long sensorId) {
        return sensorReadingService.findHistory(sensorId);
    }
}
