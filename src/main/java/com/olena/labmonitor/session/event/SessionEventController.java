package com.olena.labmonitor.session.event;

import com.olena.labmonitor.session.event.dto.CreateSessionEventRequest;
import com.olena.labmonitor.session.event.dto.SessionEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import com.olena.labmonitor.session.MonitoringSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-sessions/{sessionId}/events")
public class SessionEventController {
    private final SessionEventService service;
    private final MonitoringSessionService sessionService;
    private final AccessPolicy accessPolicy;

    public SessionEventController(SessionEventService service, MonitoringSessionService sessionService,
                                  AccessPolicy accessPolicy) {
        this.service = service; this.sessionService = sessionService; this.accessPolicy = accessPolicy;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionEventResponse create(@PathVariable Long sessionId,
                                       @Valid @RequestBody CreateSessionEventRequest request,
                                       @AuthenticationPrincipal UserDetails user,
                                       Authentication authentication) {
        requireSessionAccess(sessionId, authentication);
        return service.create(sessionId, request, user.getUsername());
    }

    @GetMapping
    public List<SessionEventResponse> findAll(@PathVariable Long sessionId, Authentication authentication) {
        requireSessionAccess(sessionId, authentication);
        return service.findAll(sessionId);
    }

    private void requireSessionAccess(Long sessionId, Authentication authentication) {
        var session = sessionService.findById(sessionId);
        accessPolicy.forAuthentication(authentication).requireViewRoom(
                session.organizationId(), session.labId(), session.roomId());
    }
}
