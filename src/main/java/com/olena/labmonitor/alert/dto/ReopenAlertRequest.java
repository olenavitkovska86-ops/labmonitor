package com.olena.labmonitor.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReopenAlertRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
