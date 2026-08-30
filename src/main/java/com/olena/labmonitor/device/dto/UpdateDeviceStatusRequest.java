package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.device.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeviceStatusRequest(@NotNull DeviceStatus status) {}
