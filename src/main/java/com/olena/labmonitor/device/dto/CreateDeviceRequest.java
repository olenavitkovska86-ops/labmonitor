package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.device.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
        @NotNull Long organizationId,
        @NotBlank @Size(max = 150) String name,
        @NotNull DeviceType type
) {}
