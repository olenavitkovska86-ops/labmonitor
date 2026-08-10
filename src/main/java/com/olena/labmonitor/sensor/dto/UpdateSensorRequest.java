package com.olena.labmonitor.sensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSensorRequest(
        @NotBlank(message = "Sensor name is required")
        @Size(max = 150, message = "Sensor name must not be longer than 150 characters")
        String name,

        @Size(max = 30, message = "Sensor unit must not be longer than 30 characters")
        String unit
) {
}
