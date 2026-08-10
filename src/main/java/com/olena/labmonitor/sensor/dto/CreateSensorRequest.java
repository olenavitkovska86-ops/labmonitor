package com.olena.labmonitor.sensor.dto;

import com.olena.labmonitor.sensor.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSensorRequest(
        @NotNull(message = "Room id is required")
        Long roomId,

        @NotBlank(message = "Sensor name is required")
        @Size(max = 150, message = "Sensor name must not be longer than 150 characters")
        String name,

        @NotNull(message = "Sensor type is required")
        SensorType type,

        @Size(max = 30, message = "Sensor unit must not be longer than 30 characters")
        String unit
) {
}
