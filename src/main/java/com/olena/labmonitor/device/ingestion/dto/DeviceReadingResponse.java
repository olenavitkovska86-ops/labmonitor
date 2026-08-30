package com.olena.labmonitor.device.ingestion.dto;

import java.time.LocalDateTime;

public record DeviceReadingResponse(
        String status, Long readingId, Long sensorId, LocalDateTime measuredAt, LocalDateTime ingestedAt
) {
    public static DeviceReadingResponse alreadyProcessed(Long readingId, Long sensorId,
                                                          LocalDateTime measuredAt, LocalDateTime ingestedAt) {
        return new DeviceReadingResponse("already_processed", readingId, sensorId, measuredAt, ingestedAt);
    }
}
