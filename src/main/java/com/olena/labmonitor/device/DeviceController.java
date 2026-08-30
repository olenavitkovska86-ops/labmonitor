package com.olena.labmonitor.device;

import com.olena.labmonitor.device.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{deviceId}/sensors/{sensorId}")
    public DeviceChannelResponse assignChannel(@PathVariable Long deviceId, @PathVariable Long sensorId,
                                               @Valid @RequestBody AssignDeviceChannelRequest request) {
        return deviceService.assignChannel(deviceId, sensorId, request);
    }
}
