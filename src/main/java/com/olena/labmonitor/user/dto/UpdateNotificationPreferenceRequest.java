package com.olena.labmonitor.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull Boolean alertNotificationsEnabled
) {
}
