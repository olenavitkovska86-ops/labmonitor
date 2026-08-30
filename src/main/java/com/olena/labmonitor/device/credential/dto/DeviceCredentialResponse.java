package com.olena.labmonitor.device.credential.dto;

import com.olena.labmonitor.device.credential.DeviceCredential;
import com.olena.labmonitor.device.credential.DeviceCredentialStatus;
import java.time.LocalDateTime;

public record DeviceCredentialResponse(
        Long id, Long deviceId, DeviceCredentialStatus status, LocalDateTime issuedAt,
        LocalDateTime lastUsedAt, LocalDateTime revokedAt, LocalDateTime createdAt
) {
    public static DeviceCredentialResponse from(DeviceCredential credential) {
        return new DeviceCredentialResponse(credential.getId(), credential.getDevice().getId(),
                credential.getStatus(), credential.getIssuedAt(), credential.getLastUsedAt(),
                credential.getRevokedAt(), credential.getCreatedAt());
    }
}
