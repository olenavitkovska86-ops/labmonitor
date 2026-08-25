package com.olena.labmonitor.simulator;

import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorType;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SensorValueScenario {

    private static final int CYCLE_LENGTH = 24;
    private static final String[] TEMPERATURE_CURVE = {
            "0.50", "0.51", "0.53", "0.56", "0.60", "0.65",
            "0.71", "0.78", "0.86", "0.94", "1.00", "1.04",
            "1.10", "1.18", "1.28", "1.38", "1.40", "1.36",
            "1.28", "1.18", "1.08", "0.98", "0.82", "0.65"
    };
    private static final String[] HUMIDITY_CURVE = {
            "0.45", "0.46", "0.48", "0.50", "0.53", "0.57",
            "0.62", "0.68", "0.75", "0.83", "0.91", "0.97",
            "1.01", "1.05", "1.10", "1.16", "1.22", "1.28",
            "1.25", "1.18", "1.10", "1.02", "0.90", "0.68"
    };

    BigDecimal valueFor(Sensor sensor, long step) {
        int position = Math.floorMod(step + sensorOffset(sensor), CYCLE_LENGTH);
        BigDecimal width = rangeWidth(sensor);
        if (sensor.getMinSafeValue() != null && sensor.getMaxSafeValue() != null) {
            if (sensor.getType() == SensorType.TEMPERATURE) {
                return curveValue(sensor, width, TEMPERATURE_CURVE[position]);
            }
            if (sensor.getType() == SensorType.HUMIDITY) {
                return curveValue(sensor, width, HUMIDITY_CURVE[position]);
            }
        }

        BigDecimal normal = normalValue(sensor, width);
        int fallbackPosition = position % 12;
        if (sensor.getId() != null && sensor.getId() % 2 == 0) {
            return switch (fallbackPosition) {
                case 6, 7 -> unsafeValue(sensor, width, new BigDecimal("0.03"));
                default -> normal;
            };
        }

        return switch (fallbackPosition) {
            case 6 -> unsafeValue(sensor, width, new BigDecimal("0.03"));
            case 7 -> unsafeValue(sensor, width, new BigDecimal("0.10"));
            case 8 -> unsafeValue(sensor, width, new BigDecimal("0.22"));
            case 9 -> unsafeValue(sensor, width, new BigDecimal("0.40"));
            case 10 -> unsafeValue(sensor, width, new BigDecimal("0.18"));
            default -> normal;
        };
    }

    private BigDecimal curveValue(Sensor sensor, BigDecimal width, String fraction) {
        return sensor.getMinSafeValue().add(width.multiply(new BigDecimal(fraction)))
                .setScale(3, RoundingMode.HALF_UP);
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
