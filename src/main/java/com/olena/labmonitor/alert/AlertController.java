package com.olena.labmonitor.alert;

import com.olena.labmonitor.alert.dto.AlertResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long sensorId,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity
    ) {
        return alertService.findAll(organizationId, labId, roomId, sensorId, status, severity);
    }

    @GetMapping("/{id}")
    public AlertResponse findById(@PathVariable Long id) {
        return alertService.findById(id);
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return alertService.acknowledge(id, userDetails.getUsername());
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return alertService.resolve(id, userDetails.getUsername());
    }
}
