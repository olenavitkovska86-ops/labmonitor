package com.olena.labmonitor.sensor.dto;

import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorStatus;
import com.olena.labmonitor.sensor.SensorType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorResponse(
        Long id,
        Long roomId,
        Long labId,
        Long organizationId,
        Long deviceId,
        String channelKey,
        String name,
        SensorType type,
        String unit,
        SensorStatus status,
        BigDecimal minSafeValue,
        BigDecimal maxSafeValue,
        LocalDateTime lastSeenAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SensorResponse from(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getRoom().getId(),
                sensor.getRoom().getLab().getId(),
                sensor.getRoom().getLab().getOrganization().getId(),
                sensor.getDevice() == null ? null : sensor.getDevice().getId(),
                sensor.getChannelKey(),
                sensor.getName(),
                sensor.getType(),
                sensor.getUnit(),
                sensor.getStatus(),
                sensor.getMinSafeValue(),
                sensor.getMaxSafeValue(),
                sensor.getLastSeenAt(),
                sensor.isActive(),
                sensor.getCreatedAt(),
                sensor.getUpdatedAt()
        );
    }
}
