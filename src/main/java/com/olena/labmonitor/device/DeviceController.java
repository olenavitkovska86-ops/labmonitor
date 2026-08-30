package com.olena.labmonitor.device;

import com.olena.labmonitor.device.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) { this.deviceService = deviceService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse create(@Valid @RequestBody CreateDeviceRequest request) {
        return deviceService.create(request);
    }

    @GetMapping
    public List<DeviceResponse> findAll(@RequestParam(required = false) Long organizationId) {
        return deviceService.findAll(organizationId);
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse findById(@PathVariable Long deviceId) {
        return deviceService.findById(deviceId);
    }

    @PatchMapping("/{deviceId}/status")
    public DeviceResponse updateStatus(@PathVariable Long deviceId,
                                       @Valid @RequestBody UpdateDeviceStatusRequest request) {
        return deviceService.updateStatus(deviceId, request);
    }

    @GetMapping("/{deviceId}/channels")
    public List<DeviceChannelResponse> findChannels(@PathVariable Long deviceId) {
        return deviceService.findChannels(deviceId);
    }

    @PutMapping("/{deviceId}/sensors/{sensorId}")
    public DeviceChannelResponse assignChannel(@PathVariable Long deviceId, @PathVariable Long sensorId,
                                               @Valid @RequestBody AssignDeviceChannelRequest request) {
        return deviceService.assignChannel(deviceId, sensorId, request);
    }

    @PostMapping("/{deviceId}/sensor-channels")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceChannelResponse createSensorChannel(
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateDeviceSensorChannelRequest request) {
        return deviceService.createSensorChannel(deviceId, request);
    }


    @DeleteMapping("/{deviceId}/sensors/{sensorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearChannel(@PathVariable Long deviceId, @PathVariable Long sensorId) {
        deviceService.clearChannel(deviceId, sensorId);
    }
}
