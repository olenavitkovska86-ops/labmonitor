package com.olena.labmonitor.sensor.reading.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateSensorReadingRequest(
        @NotNull(message = "Sensor id is required")
        Long sensorId,

        @NotNull(message = "Sensor reading value is required")
        BigDecimal value,

        @PastOrPresent(message = "Measurement time must not be in the future")
        LocalDateTime measuredAt
) {
}
