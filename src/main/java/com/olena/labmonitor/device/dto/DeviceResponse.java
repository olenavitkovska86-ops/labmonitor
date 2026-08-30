package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.device.Device;
import com.olena.labmonitor.device.DeviceStatus;
import com.olena.labmonitor.device.DeviceType;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id, Long roomId, String roomName, Long labId, String labName,
        Long organizationId, String organizationName, String name, DeviceType type, DeviceStatus status,
        LocalDateTime lastSeenAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static DeviceResponse from(Device device) {
        var room = device.getRoom();
        var lab = room.getLab();
        var organization = lab.getOrganization();
        return new DeviceResponse(device.getId(), room.getId(), room.getName(), lab.getId(), lab.getName(),
                organization.getId(), organization.getName(), device.getName(),
                device.getType(), device.getStatus(), device.getLastSeenAt(), device.getCreatedAt(),
                device.getUpdatedAt());
    }
}
