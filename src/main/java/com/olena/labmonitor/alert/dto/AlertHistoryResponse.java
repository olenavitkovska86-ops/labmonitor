package com.olena.labmonitor.alert.dto;

import com.olena.labmonitor.alert.AlertResolutionOutcome;
import com.olena.labmonitor.alert.history.AlertHistory;
import com.olena.labmonitor.alert.history.AlertHistoryAction;
import com.olena.labmonitor.user.User;

import java.time.LocalDateTime;

public record AlertHistoryResponse(
        AlertHistoryAction action,
        AlertResolutionOutcome resolutionOutcome,
        String comment,
        LocalDateTime createdAt,
        Long performedByUserId,
        String performedByName
) {
    public static AlertHistoryResponse from(AlertHistory history) {
        User user = history.getPerformedByUser();
        return new AlertHistoryResponse(
                history.getAction(),
                history.getResolutionOutcome(),
                history.getComment(),
                history.getCreatedAt(),
                user == null ? null : user.getId(),
                displayName(user)
        );
    }

    private static String displayName(User user) {
        if (user == null) return null;
        String name = (user.getFirstName() + " " + user.getLastName()).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
