package com.olena.labmonitor.device.credential.dto;

import com.olena.labmonitor.device.credential.DeviceCredentialStatus;
import java.time.LocalDateTime;

public record ProvisionedDeviceCredentialResponse(
        Long credentialId, Long deviceId, String token, DeviceCredentialStatus status, LocalDateTime issuedAt
) {}
