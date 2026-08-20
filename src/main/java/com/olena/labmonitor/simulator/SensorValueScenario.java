package com.olena.labmonitor.simulator;

import com.olena.labmonitor.sensor.Sensor;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SensorValueScenario {

    private static final int CYCLE_LENGTH = 12;

    BigDecimal valueFor(Sensor sensor, long step) {
        int position = Math.floorMod(step + sensorOffset(sensor), CYCLE_LENGTH);
        BigDecimal width = rangeWidth(sensor);
        BigDecimal normal = normalValue(sensor, width);

        if (sensor.getId() != null && sensor.getId() % 2 == 0) {
            return switch (position) {
                case 6, 7 -> unsafeValue(sensor, width, new BigDecimal("0.03"));
                default -> normal;
            };
        }

        return switch (position) {
            case 6 -> unsafeValue(sensor, width, new BigDecimal("0.03"));
            case 7 -> unsafeValue(sensor, width, new BigDecimal("0.10"));
            case 8 -> unsafeValue(sensor, width, new BigDecimal("0.22"));
            case 9 -> unsafeValue(sensor, width, new BigDecimal("0.40"));
            case 10 -> unsafeValue(sensor, width, new BigDecimal("0.18"));
            default -> normal;
        };
    }

    private int sensorOffset(Sensor sensor) {
        return sensor.getId() == null ? 0 : Math.floorMod(sensor.getId().intValue(), CYCLE_LENGTH);
    }

    private BigDecimal rangeWidth(Sensor sensor) {
        if (sensor.getMinSafeValue() != null && sensor.getMaxSafeValue() != null) {
            return sensor.getMaxSafeValue().subtract(sensor.getMinSafeValue());
        }
        BigDecimal boundary = sensor.getMaxSafeValue() != null
                ? sensor.getMaxSafeValue().abs()
                : sensor.getMinSafeValue().abs();
        return boundary.multiply(new BigDecimal("0.10")).max(BigDecimal.ONE);
    }

    private BigDecimal normalValue(Sensor sensor, BigDecimal width) {
        BigDecimal value;
        if (sensor.getMinSafeValue() != null && sensor.getMaxSafeValue() != null) {
            value = sensor.getMinSafeValue().add(sensor.getMaxSafeValue())
                    .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
        } else if (sensor.getMaxSafeValue() != null) {
            value = sensor.getMaxSafeValue().subtract(width.multiply(new BigDecimal("0.25")));
        } else {
            value = sensor.getMinSafeValue().add(width.multiply(new BigDecimal("0.25")));
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal unsafeValue(Sensor sensor, BigDecimal width, BigDecimal deviationFactor) {
        BigDecimal deviation = width.multiply(deviationFactor);
        BigDecimal value = sensor.getMaxSafeValue() != null
                ? sensor.getMaxSafeValue().add(deviation)
                : sensor.getMinSafeValue().subtract(deviation);
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}
