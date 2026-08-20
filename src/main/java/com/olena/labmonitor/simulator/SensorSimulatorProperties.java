package com.olena.labmonitor.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("labmonitor.simulator")
public class SensorSimulatorProperties {

    private int maxSensors = 20;

    public int getMaxSensors() { return maxSensors; }
    public void setMaxSensors(int maxSensors) { this.maxSensors = maxSensors; }
}
