package com.olena.labmonitor.session.event;

import com.olena.labmonitor.session.event.dto.CreateSessionEventRequest;
import com.olena.labmonitor.session.event.dto.SessionEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-sessions/{sessionId}/events")
public class SessionEventController {
    private final SessionEventService service;

    public SessionEventController(SessionEventService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionEventResponse create(@PathVariable Long sessionId,
                                       @Valid @RequestBody CreateSessionEventRequest request,
                                       @AuthenticationPrincipal UserDetails user) {
        return service.create(sessionId, request, user.getUsername());
    }

    @GetMapping
    public List<SessionEventResponse> findAll(@PathVariable Long sessionId) {
        return service.findAll(sessionId);
    }
}
