package com.olena.labmonitor.device.ingestion;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.device.*;
import com.olena.labmonitor.device.credential.*;
import com.olena.labmonitor.device.ingestion.dto.*;
import com.olena.labmonitor.device.security.DevicePrincipal;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingService;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Transactional
public class DeviceIngestionService {
    private final DeviceRepository deviceRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SensorReadingService readingService;

    public DeviceIngestionService(DeviceRepository deviceRepository,
                                  DeviceCredentialRepository credentialRepository,
                                  SensorRepository sensorRepository,
                                  SensorReadingRepository readingRepository,
                                  SensorReadingService readingService) {
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.readingService = readingService;
    }

    public DeviceReadingResponse ingest(DevicePrincipal principal, DeviceReadingRequest request) {
        Device device = deviceRepository.findByIdForUpdate(principal.deviceId())
                .orElseThrow(() -> new BadCredentialsException("Invalid device credential"));
        DeviceCredential credential = credentialRepository.findById(principal.credentialId())
                .filter(DeviceCredential::isActive)
                .filter(item -> item.getDevice().getId().equals(device.getId()))
                .orElseThrow(() -> new BadCredentialsException("Invalid device credential"));
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new BadCredentialsException("Device is disabled");
        }

        LocalDateTime receivedAt = LocalDateTime.now(ZoneOffset.UTC);
        SensorReading existing = readingRepository
                .findBySourceDeviceIdAndMessageId(device.getId(), request.messageId()).orElse(null);
        device.recordSeen(receivedAt);
        credential.recordUsed(receivedAt);
        if (existing != null) {
            return DeviceReadingResponse.alreadyProcessed(existing.getId(), existing.getSensor().getId(),
                    existing.getMeasuredAt(), existing.getCreatedAt());
        }

        String channel = DeviceService.normalizeChannel(request.channel());
        Sensor sensor = sensorRepository.findByDeviceIdAndChannelKey(device.getId(), channel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Channel '" + channel + "' is not configured for this device"));
        LocalDateTime measuredAt = request.measuredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        SensorReadingResponse reading = readingService.createFromDevice(
                sensor, request.value(), measuredAt, device, request.messageId());
        return new DeviceReadingResponse("accepted", reading.id(), reading.sensorId(),
                reading.measuredAt(), reading.createdAt());
    }
}
