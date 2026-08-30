package com.olena.labmonitor.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignDeviceChannelRequest(@NotBlank @Size(max = 100) String channelKey) {}
