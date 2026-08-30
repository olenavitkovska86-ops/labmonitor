package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.sensor.SensorType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateDeviceSensorChannelRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull SensorType type,
        @Size(max = 30) String unit,
        @NotBlank @Size(max = 100) String channelKey,
        BigDecimal minSafeValue,
        BigDecimal maxSafeValue
) {
    @AssertTrue(message = "Minimum safe value must be less than maximum safe value")
    public boolean isSafeRangeValid() {
        return minSafeValue == null || maxSafeValue == null || minSafeValue.compareTo(maxSafeValue) < 0;
    }
}
