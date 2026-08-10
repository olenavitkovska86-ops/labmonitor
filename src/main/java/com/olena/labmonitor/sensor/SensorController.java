package com.olena.labmonitor.sensor;

import com.olena.labmonitor.sensor.dto.CreateSensorRequest;
import com.olena.labmonitor.sensor.dto.SensorResponse;
import com.olena.labmonitor.sensor.dto.UpdateSensorRequest;
import com.olena.labmonitor.sensor.dto.UpdateSensorSafeRangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorResponse create(@Valid @RequestBody CreateSensorRequest request) {
        return sensorService.create(request);
    }

    @GetMapping
    public List<SensorResponse> findAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String search
    ) {
        return sensorService.findAll(roomId, search);
    }

    @GetMapping("/{id}")
    public SensorResponse findById(@PathVariable Long id) {
        return sensorService.findById(id);
    }

    @PutMapping("/{id}")
    public SensorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSensorRequest request) {
        return sensorService.update(id, request);
    }

    @PutMapping("/{id}/safe-range")
    public SensorResponse updateSafeRange(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSensorSafeRangeRequest request
    ) {
        return sensorService.updateSafeRange(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public SensorResponse deactivate(@PathVariable Long id) {
        return sensorService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public SensorResponse activate(@PathVariable Long id) {
        return sensorService.activate(id);
    }
}
