package com.olena.labmonitor.device.ingestion;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.config.MonitoringProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class DeviceTimestampPolicy {
    private final Clock clock;
    private final MonitoringProperties.DeviceIngestion properties;

    public DeviceTimestampPolicy(Clock clock, MonitoringProperties monitoringProperties) {
        this.clock = clock;
        this.properties = monitoringProperties.getDeviceIngestion();
    }

    public LocalDateTime toApplicationTime(OffsetDateTime measuredAt) {
        Instant measuredInstant = measuredAt.toInstant();
        Instant now = clock.instant();
        if (measuredInstant.isBefore(now.minus(properties.getMaxPastAge()))) {
            throw new InvalidOperationException("Device reading timestamp is too far in the past");
        }
        if (measuredInstant.isAfter(now.plus(properties.getMaxFutureSkew()))) {
            throw new InvalidOperationException("Device reading timestamp is too far in the future");
        }
        return LocalDateTime.ofInstant(measuredInstant, clock.getZone());
    }
}
