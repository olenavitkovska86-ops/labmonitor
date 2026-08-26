package com.olena.labmonitor.alert;

import com.olena.labmonitor.alert.dto.AlertResponse;
import com.olena.labmonitor.alert.dto.AlertCountResponse;
import com.olena.labmonitor.alert.dto.ResolveAlertRequest;
import com.olena.labmonitor.alert.dto.ReopenAlertRequest;
import com.olena.labmonitor.alert.dto.AlertHistoryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final AccessPolicy accessPolicy;

    public AlertController(AlertService alertService, AccessPolicy accessPolicy) {
        this.alertService = alertService;
        this.accessPolicy = accessPolicy;
    }

    @GetMapping
    public List<AlertResponse> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long sensorId,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            Authentication authentication
    ) {
        var access = accessPolicy.forAuthentication(authentication);
        return alertService.findAll(organizationId, labId, roomId, sensorId, status, severity).stream()
                .filter(alert -> access.canViewRoom(alert.organizationId(), alert.labId(), alert.roomId()))
                .toList();
    }

    @GetMapping("/{id}")
    public AlertResponse findById(@PathVariable Long id, Authentication authentication) {
        AlertResponse alert = requireAlertAccess(id, authentication);
        return alert;
    }

    @GetMapping("/{id}/history")
    public List<AlertHistoryResponse> findHistory(@PathVariable Long id, Authentication authentication) {
        requireAlertAccess(id, authentication);
        return alertService.findHistory(id);
    }

    @GetMapping("/unresolved-count")
    public AlertCountResponse countUnresolved(Authentication authentication) {
        long count = findAll(null, null, null, null, null, null, authentication).stream()
                .filter(alert -> alert.status() == AlertStatus.ACTIVE || alert.status() == AlertStatus.ACKNOWLEDGED)
                .count();
        return new AlertCountResponse(count);
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                                     Authentication authentication) {
        requireAlertAccess(id, authentication);
        return alertService.acknowledge(id, userDetails.getUsername());
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ResolveAlertRequest request,
            Authentication authentication
    ) {
        requireAlertAccess(id, authentication);
        return alertService.resolve(id, userDetails.getUsername(), request);
    }

    @PostMapping("/{id}/reopen")
    public AlertResponse reopen(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReopenAlertRequest request,
            Authentication authentication
    ) {
        requireAlertAccess(id, authentication);
        return alertService.reopen(id, userDetails.getUsername(), request);
    }

    private AlertResponse requireAlertAccess(Long id, Authentication authentication) {
        AlertResponse alert = alertService.findById(id);
        accessPolicy.forAuthentication(authentication)
                .requireViewRoom(alert.organizationId(), alert.labId(), alert.roomId());
        return alert;
    }
}
