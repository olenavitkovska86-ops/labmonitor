package com.olena.labmonitor.alert.dto;

import com.olena.labmonitor.alert.AlertResolutionOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveAlertRequest(
        @NotNull AlertResolutionOutcome outcome,
        @Size(max = 1000) String comment
) {
}
