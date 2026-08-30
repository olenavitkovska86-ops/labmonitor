package com.olena.labmonitor.device;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.device.dto.*;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final RoomService roomService;
    private final SensorService sensorService;
    private final SensorRepository sensorRepository;

    public DeviceService(DeviceRepository deviceRepository, RoomService roomService,
                         SensorService sensorService, SensorRepository sensorRepository) {
        this.deviceRepository = deviceRepository;
        this.roomService = roomService;
        this.sensorService = sensorService;
        this.sensorRepository = sensorRepository;
    }

    public DeviceResponse create(CreateDeviceRequest request) {
        Device device = new Device(roomService.getExistingRoom(request.roomId()),
                request.name().trim(), request.type());
        return DeviceResponse.from(deviceRepository.saveAndFlush(device));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> findAll(Long organizationId) {
        List<Device> devices = organizationId == null
                ? deviceRepository.findAllByOrderByNameAsc()
                : deviceRepository.findByRoomLabOrganizationIdOrderByNameAsc(organizationId);
        return devices.stream().map(DeviceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse findById(Long id) {
        return DeviceResponse.from(getExistingDevice(id));
    }

    @Transactional
    public DeviceResponse updateStatus(Long id, UpdateDeviceStatusRequest request) {
        Device device = getExistingDeviceForUpdate(id);
        if (request.status() == DeviceStatus.ACTIVE) device.activate();
        else device.disable();
        return DeviceResponse.from(deviceRepository.saveAndFlush(device));
    }

    public DeviceChannelResponse assignChannel(Long deviceId, Long sensorId, AssignDeviceChannelRequest request) {
        Device device = getExistingDevice(deviceId);
        Sensor sensor = sensorService.getExistingSensor(sensorId);
        if (!device.getRoom().getId().equals(sensor.getRoom().getId())) {
            throw new InvalidOperationException("Device and sensor must belong to the same room");
        }
        if (sensor.getDevice() != null && !sensor.getDevice().getId().equals(deviceId)) {
            throw new InvalidOperationException("Sensor is already assigned to another device");
        }
        String channel = normalizeChannel(request.channelKey());
        sensorRepository.findByDeviceIdAndChannelKey(deviceId, channel)
                .filter(existing -> !existing.getId().equals(sensorId))
                .ifPresent(existing -> { throw new InvalidOperationException(
                        "Channel '" + channel + "' is already assigned to sensor with id " + existing.getId()); });
        sensor.assignDeviceChannel(device, channel);
        sensorRepository.saveAndFlush(sensor);
        return DeviceChannelResponse.from(sensor);
    }

    public DeviceChannelResponse createSensorChannel(Long deviceId, CreateDeviceSensorChannelRequest request) {
        Device device = getExistingDevice(deviceId);
        String channel = normalizeChannel(request.channelKey());
        sensorRepository.findByDeviceIdAndChannelKey(deviceId, channel)
                .ifPresent(existing -> { throw new InvalidOperationException(
                        "Channel '" + channel + "' is already assigned to sensor with id " + existing.getId()); });
        Sensor sensor = new Sensor(device.getRoom(), request.name().trim(), request.type(), trimToNull(request.unit()));
        sensor.updateSafeRange(request.minSafeValue(), request.maxSafeValue());
        sensor.assignDeviceChannel(device, channel);
        return DeviceChannelResponse.from(sensorRepository.saveAndFlush(sensor));
    }

    @Transactional(readOnly = true)
    public List<DeviceChannelResponse> findChannels(Long deviceId) {
        getExistingDevice(deviceId);
        return sensorRepository.findByDeviceId(deviceId).stream()
                .map(DeviceChannelResponse::from).toList();
    }

    @Transactional
    public void clearChannel(Long deviceId, Long sensorId) {
        Sensor sensor = sensorService.getExistingSensor(sensorId);
        if (sensor.getDevice() == null || !sensor.getDevice().getId().equals(deviceId)) {
            throw new ResourceNotFoundException("Sensor with id " + sensorId
                    + " is not assigned to device with id " + deviceId);
        }
        sensor.clearDeviceChannel();
        sensorRepository.saveAndFlush(sensor);
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
