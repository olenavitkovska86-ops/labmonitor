package com.olena.labmonitor.sensor;

import com.olena.labmonitor.sensor.dto.CreateSensorRequest;
import com.olena.labmonitor.sensor.dto.SensorResponse;
import com.olena.labmonitor.sensor.dto.UpdateSensorRequest;
import com.olena.labmonitor.sensor.dto.UpdateSensorSafeRangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
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
    private final AccessPolicy accessPolicy;

    public SensorController(SensorService sensorService, AccessPolicy accessPolicy) {
        this.sensorService = sensorService;
        this.accessPolicy = accessPolicy;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorResponse create(@Valid @RequestBody CreateSensorRequest request) {
        return sensorService.create(request);
    }

    @GetMapping
    public List<SensorResponse> findAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        var access = accessPolicy.forAuthentication(authentication);
        return sensorService.findAll(roomId, search).stream()
                .filter(sensor -> access.canViewRoom(
                        sensor.organizationId(), sensor.labId(), sensor.roomId()))
                .toList();
    }

    @GetMapping("/{id}")
    public SensorResponse findById(@PathVariable Long id, Authentication authentication) {
        var sensor = sensorService.findById(id);
        accessPolicy.forAuthentication(authentication).requireViewRoom(
                sensor.organizationId(), sensor.labId(), sensor.roomId());
        return sensor;
    }

    @PutMapping("/{id}")
    public SensorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSensorRequest request,
                                 Authentication authentication) {
        requireSensorManagement(id, authentication);
        return sensorService.update(id, request);
    }

    @PutMapping("/{id}/safe-range")
    public SensorResponse updateSafeRange(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSensorSafeRangeRequest request,
            Authentication authentication
    ) {
        requireSensorManagement(id, authentication);
        return sensorService.updateSafeRange(id, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/deactivate")
    public SensorResponse deactivate(@PathVariable Long id) {
        return sensorService.deactivate(id);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/activate")
    public SensorResponse activate(@PathVariable Long id) {
        return sensorService.activate(id);
    }

    private void requireSensorManagement(Long id, Authentication authentication) {
        SensorResponse sensor = sensorService.findById(id);
        accessPolicy.forAuthentication(authentication).requireManageSensor(
                sensor.organizationId(), sensor.labId(), sensor.roomId());
    }
}
