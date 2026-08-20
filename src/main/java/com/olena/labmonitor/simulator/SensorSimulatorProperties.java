package com.olena.labmonitor.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("labmonitor.simulator")
public class SensorSimulatorProperties {

    private boolean enabled;
    private Duration interval = Duration.ofMinutes(1);
    private Duration startupDelay = Duration.ofSeconds(10);
    private int maxSensors = 20;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public Duration getStartupDelay() { return startupDelay; }
    public void setStartupDelay(Duration startupDelay) { this.startupDelay = startupDelay; }
    public int getMaxSensors() { return maxSensors; }
    public void setMaxSensors(int maxSensors) { this.maxSensors = maxSensors; }
}
