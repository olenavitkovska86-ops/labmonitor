package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.device.Device;
import com.olena.labmonitor.device.DeviceStatus;
import com.olena.labmonitor.device.DeviceType;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id, Long organizationId, String name, DeviceType type, DeviceStatus status,
        LocalDateTime lastSeenAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getOrganization().getId(), device.getName(),
                device.getType(), device.getStatus(), device.getLastSeenAt(), device.getCreatedAt(),
                device.getUpdatedAt());
    }
}
