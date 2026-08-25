package com.olena.labmonitor.session.timeline;

import com.olena.labmonitor.alert.dto.AlertResponse;
import com.olena.labmonitor.sensor.reading.SensorReading;
import com.olena.labmonitor.sensor.reading.SensorReadingStatus;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.session.dto.MonitoringSessionResponse;
import com.olena.labmonitor.session.event.dto.SessionEventResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionTimelineResponse(
        MonitoringSessionResponse session,
        LocalDateTime from,
        LocalDateTime to,
        List<TimelineReading> readings,
        List<SessionEventResponse> events,
        List<AlertResponse> alerts,
        boolean readingsTruncated
) {
    public record TimelineReading(
            Long id,
            Long sensorId,
            String sensorName,
            SensorType sensorType,
            BigDecimal value,
            String unit,
            BigDecimal safeMin,
            BigDecimal safeMax,
            SensorReadingStatus status,
            LocalDateTime measuredAt
    ) {
        public static TimelineReading from(SensorReading reading) {
            var sensor = reading.getSensor();
            return new TimelineReading(reading.getId(), sensor.getId(), sensor.getName(), sensor.getType(),
                    reading.getValue(), sensor.getUnit(), reading.getSafeMin(), reading.getSafeMax(),
                    reading.getStatus(), reading.getMeasuredAt());
        }
    }
}
