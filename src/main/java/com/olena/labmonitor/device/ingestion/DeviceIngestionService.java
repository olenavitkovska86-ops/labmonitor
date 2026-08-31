package com.olena.labmonitor.device.ingestion;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.device.*;
import com.olena.labmonitor.device.credential.*;
import com.olena.labmonitor.device.ingestion.dto.*;
import com.olena.labmonitor.device.security.DevicePrincipal;
import com.olena.labmonitor.device.security.InvalidDeviceCredentialException;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingService;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class DeviceIngestionService {
    private final DeviceRepository deviceRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SensorReadingService readingService;
    private final Clock clock;
    private final DeviceTimestampPolicy timestampPolicy;

    public DeviceIngestionService(DeviceRepository deviceRepository,
                                  DeviceCredentialRepository credentialRepository,
                                  SensorRepository sensorRepository,
                                  SensorReadingRepository readingRepository,
                                  SensorReadingService readingService,
                                  Clock clock,
                                  DeviceTimestampPolicy timestampPolicy) {
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.readingService = readingService;
        this.clock = clock;
        this.timestampPolicy = timestampPolicy;
    }

    public DeviceReadingResponse ingest(DevicePrincipal principal, DeviceReadingRequest request) {
        Device device = deviceRepository.findByIdForUpdate(principal.deviceId())
                .orElseThrow(InvalidDeviceCredentialException::new);
        DeviceCredential credential = credentialRepository.findById(principal.credentialId())
                .filter(DeviceCredential::isActive)
                .filter(item -> item.getDevice().getId().equals(device.getId()))
                .orElseThrow(InvalidDeviceCredentialException::new);
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new InvalidDeviceCredentialException();
        }

        LocalDateTime receivedAt = LocalDateTime.now(clock);
        SensorReading existing = readingRepository
                .findBySourceDeviceIdAndMessageId(device.getId(), request.messageId()).orElse(null);
        device.recordSeen(receivedAt);
        credential.recordUsed(receivedAt);
        if (existing != null) {
            return DeviceReadingResponse.alreadyProcessed(existing.getId(), existing.getSensor().getId(),
                    existing.getMeasuredAt(), existing.getCreatedAt());
        }

        String channel = DeviceService.normalizeChannel(request.channel());
        Sensor sensor = sensorRepository.findByDeviceIdAndChannelKeyForUpdate(device.getId(), channel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Channel '" + channel + "' is not configured for this device"));
        LocalDateTime measuredAt = timestampPolicy.toApplicationTime(request.measuredAt());
        SensorReadingResponse reading = readingService.createFromDevice(
                sensor, request.value(), measuredAt, device, request.messageId());
        return new DeviceReadingResponse("accepted", reading.id(), reading.sensorId(),
                reading.measuredAt(), reading.createdAt());
    }
}
