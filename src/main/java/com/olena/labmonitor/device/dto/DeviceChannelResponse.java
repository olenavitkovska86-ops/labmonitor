package com.olena.labmonitor.device.dto;

import com.olena.labmonitor.sensor.Sensor;

public record DeviceChannelResponse(
        Long deviceId, Long sensorId, String sensorName, Long roomId, String roomName, String channelKey
) {
    public static DeviceChannelResponse from(Sensor sensor) {
        return new DeviceChannelResponse(sensor.getDevice().getId(), sensor.getId(), sensor.getName(),
                sensor.getRoom().getId(), sensor.getRoom().getName(), sensor.getChannelKey());
    }
}
