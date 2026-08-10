package com.olena.labmonitor.sensor.reading.dto;

import com.olena.labmonitor.sensor.reading.SensorReading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorReadingResponse(
        Long id,
        Long sensorId,
        Long roomId,
        BigDecimal value,
        String unit,
        LocalDateTime measuredAt,
        LocalDateTime createdAt
) {

    public static SensorReadingResponse from(SensorReading reading) {
        return new SensorReadingResponse(
                reading.getId(),
                reading.getSensor().getId(),
                reading.getRoom().getId(),
                reading.getValue(),
                reading.getSensor().getUnit(),
                reading.getMeasuredAt(),
                reading.getCreatedAt()
        );
    }
}
