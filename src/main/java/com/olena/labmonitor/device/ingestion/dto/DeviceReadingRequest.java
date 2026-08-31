package com.olena.labmonitor.device.ingestion.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DeviceReadingRequest(
        @NotBlank @Size(max = 100) String channel,
        @NotNull @Digits(integer = 9, fraction = 3) BigDecimal value,
        @NotNull OffsetDateTime measuredAt,
        @NotBlank @Size(max = 100) String messageId
) {}
