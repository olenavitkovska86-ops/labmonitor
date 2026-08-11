package com.olena.labmonitor.sensor.dto;

import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record UpdateSensorSafeRangeRequest(
        BigDecimal minSafeValue,
        BigDecimal maxSafeValue
) {

    @AssertTrue(message = "Minimum safe value must be less than maximum safe value")
    public boolean isSafeRangeValid() {
        return minSafeValue == null
                || maxSafeValue == null
                || minSafeValue.compareTo(maxSafeValue) < 0;
    }
}
