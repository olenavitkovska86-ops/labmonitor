package com.olena.labmonitor.session;

import com.olena.labmonitor.session.dto.CreateMonitoringSessionRequest;
import com.olena.labmonitor.session.dto.MonitoringSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-sessions")
public class MonitoringSessionController {

    private final MonitoringSessionService service;

    public MonitoringSessionController(MonitoringSessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoringSessionResponse create(@Valid @RequestBody CreateMonitoringSessionRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        return service.create(request, user.getUsername());
    }

    @GetMapping
    public List<MonitoringSessionResponse> findAll(@RequestParam(required = false) Long roomId,
                                                   @RequestParam(required = false) MonitoringSessionStatus status) {
        return service.findAll(roomId, status);
    }

    @GetMapping("/{id}")
    public MonitoringSessionResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping("/{id}/start")
    public MonitoringSessionResponse start(@PathVariable Long id) { return service.start(id); }

    @PostMapping("/{id}/complete")
    public MonitoringSessionResponse complete(@PathVariable Long id) { return service.complete(id); }

    @PostMapping("/{id}/cancel")
    public MonitoringSessionResponse cancel(@PathVariable Long id) { return service.cancel(id); }
}
