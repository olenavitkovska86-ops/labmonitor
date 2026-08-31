package com.olena.labmonitor.device.ingestion;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.config.MonitoringProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTimestampPolicyTests {
    private static final Instant NOW = Instant.parse("2026-08-30T20:00:00Z");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private final DeviceTimestampPolicy policy = new DeviceTimestampPolicy(
            Clock.fixed(NOW, WARSAW), new MonitoringProperties());

    @Test
    void convertsAcceptedTimestampToApplicationTimeline() {
        var measuredAt = OffsetDateTime.parse("2026-08-30T19:58:00Z");

        assertThat(policy.toApplicationTime(measuredAt))
                .isEqualTo(LocalDateTime.of(2026, 8, 30, 21, 58));
    }

    @Test
    void acceptsConfiguredBoundaryValues() {
        assertThat(policy.toApplicationTime(OffsetDateTime.parse("2026-08-30T19:55:00Z"))).isNotNull();
        assertThat(policy.toApplicationTime(OffsetDateTime.parse("2026-08-30T20:01:00Z"))).isNotNull();
    }

    @Test
    void rejectsTimestampOlderThanConfiguredWindow() {
        assertThatThrownBy(() -> policy.toApplicationTime(OffsetDateTime.parse("2026-08-30T19:54:59Z")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void rejectsTimestampTooFarInFuture() {
        assertThatThrownBy(() -> policy.toApplicationTime(OffsetDateTime.parse("2026-08-30T20:01:01Z")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("future");
    }
}
