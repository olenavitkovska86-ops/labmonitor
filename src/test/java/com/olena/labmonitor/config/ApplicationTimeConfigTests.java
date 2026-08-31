package com.olena.labmonitor.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTimeConfigTests {

    @Test
    void createsClockForConfiguredApplicationTimeZone() {
        var clock = new ApplicationTimeConfig().applicationClock("Europe/Warsaw");

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
    }
}
