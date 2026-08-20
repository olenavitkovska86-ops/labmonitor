package com.olena.labmonitor.analytics;

import com.olena.labmonitor.analytics.dto.OrganizationOverviewResponse;
import com.olena.labmonitor.analytics.dto.OrganizationHistoryResponse;
import com.olena.labmonitor.analytics.dto.ProblemRoomResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/organizations/{organizationId}")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public OrganizationOverviewResponse getOverview(@PathVariable Long organizationId) {
        return analyticsService.getOrganizationOverview(organizationId);
    }

    @GetMapping("/problem-rooms")
    public List<ProblemRoomResponse> getProblemRooms(@PathVariable Long organizationId) {
        return analyticsService.getProblemRooms(organizationId);
    }

    @GetMapping("/history")
    public OrganizationHistoryResponse getHistory(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "LAST_7_DAYS") AnalyticsPeriod period
    ) {
        return analyticsService.getOrganizationHistory(organizationId, period);
    }
}
