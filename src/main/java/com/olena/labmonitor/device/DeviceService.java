package com.olena.labmonitor.device;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.device.dto.*;
import com.olena.labmonitor.organization.OrganizationService;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final OrganizationService organizationService;
    private final SensorService sensorService;
    private final SensorRepository sensorRepository;

    public DeviceService(DeviceRepository deviceRepository, OrganizationService organizationService,
                         SensorService sensorService, SensorRepository sensorRepository) {
        this.deviceRepository = deviceRepository;
        this.organizationService = organizationService;
        this.sensorService = sensorService;
        this.sensorRepository = sensorRepository;
    }

    public DeviceResponse create(CreateDeviceRequest request) {
        Device device = new Device(organizationService.getExistingOrganization(request.organizationId()),
                request.name().trim(), request.type());
        return DeviceResponse.from(deviceRepository.saveAndFlush(device));
    }

    public DeviceChannelResponse assignChannel(Long deviceId, Long sensorId, AssignDeviceChannelRequest request) {
        Device device = getExistingDevice(deviceId);
        Sensor sensor = sensorService.getExistingSensor(sensorId);
        if (!device.getOrganization().getId().equals(sensor.getRoom().getLab().getOrganization().getId())) {
            throw new InvalidOperationException("Device and sensor must belong to the same organization");
        }
        String channel = normalizeChannel(request.channelKey());
        sensorRepository.findByDeviceIdAndChannelKey(deviceId, channel)
                .filter(existing -> !existing.getId().equals(sensorId))
                .ifPresent(existing -> { throw new InvalidOperationException(
                        "Channel '" + channel + "' is already assigned to sensor with id " + existing.getId()); });
        sensor.assignDeviceChannel(device, channel);
        sensorRepository.saveAndFlush(sensor);
        return new DeviceChannelResponse(deviceId, sensorId, channel);
    }

    public Device getExistingDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device with id " + id + " was not found"));
    }

    public Device getExistingDeviceForUpdate(Long id) {
        return deviceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device with id " + id + " was not found"));
    }

    public static String normalizeChannel(String channel) {
        return channel.trim().toLowerCase(Locale.ROOT);
    }
}
