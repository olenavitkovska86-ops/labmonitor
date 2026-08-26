package com.olena.labmonitor.analytics;

import com.olena.labmonitor.analytics.dto.OrganizationOverviewResponse;
import com.olena.labmonitor.analytics.dto.OrganizationHistoryResponse;
import com.olena.labmonitor.analytics.dto.ProblemRoomResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import com.olena.labmonitor.room.RoomService;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/organizations/{organizationId}")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AccessPolicy accessPolicy;
    private final RoomService roomService;

    public AnalyticsController(AnalyticsService analyticsService, AccessPolicy accessPolicy, RoomService roomService) {
        this.analyticsService = analyticsService;
        this.accessPolicy = accessPolicy;
        this.roomService = roomService;
    }

    @GetMapping("/overview")
    public OrganizationOverviewResponse getOverview(@PathVariable Long organizationId, Authentication authentication) {
        return analyticsService.getOrganizationOverview(organizationId, allowedRoomIds(organizationId, authentication));
    }

    @GetMapping("/problem-rooms")
    public List<ProblemRoomResponse> getProblemRooms(@PathVariable Long organizationId,
                                                     Authentication authentication) {
        return analyticsService.getProblemRooms(organizationId, allowedRoomIds(organizationId, authentication));
    }

    @GetMapping("/history")
    public OrganizationHistoryResponse getHistory(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "LAST_7_DAYS") AnalyticsPeriod period,
            Authentication authentication
    ) {
        return analyticsService.getOrganizationHistory(
                organizationId, period, allowedRoomIds(organizationId, authentication));
    }

    private Set<Long> allowedRoomIds(Long organizationId, Authentication authentication) {
        var access = accessPolicy.forAuthentication(authentication);
        access.requireViewOrganization(organizationId);
        if (access.hasOrganizationWideAccess(organizationId)) return null;
        return roomService.findAll(null, null).stream()
                .filter(room -> room.organizationId().equals(organizationId))
                .filter(room -> access.canViewRoom(room.organizationId(), room.labId(), room.id()))
                .map(room -> room.id())
                .collect(Collectors.toUnmodifiableSet());
    }
}
