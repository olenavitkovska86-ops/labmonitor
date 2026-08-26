package com.olena.labmonitor.membership.dto;

import com.olena.labmonitor.membership.MembershipScopeType;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record MembershipScopeRequest(
        @NotNull MembershipScopeType type,
        Set<Long> labIds,
        Set<Long> roomIds
) {
    public Set<Long> normalizedLabIds() {
        return labIds == null ? Set.of() : Set.copyOf(labIds);
    }

    public Set<Long> normalizedRoomIds() {
        return roomIds == null ? Set.of() : Set.copyOf(roomIds);
    }
}
