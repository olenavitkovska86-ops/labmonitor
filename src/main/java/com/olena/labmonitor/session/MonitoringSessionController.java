package com.olena.labmonitor.session;

import com.olena.labmonitor.session.dto.CreateMonitoringSessionRequest;
import com.olena.labmonitor.session.dto.MonitoringSessionResponse;
import com.olena.labmonitor.session.export.SessionExportService;
import com.olena.labmonitor.session.timeline.SessionTimelineResponse;
import com.olena.labmonitor.session.timeline.SessionTimelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-sessions")
public class MonitoringSessionController {

    private final MonitoringSessionService service;
    private final SessionTimelineService timelineService;
    private final SessionExportService exportService;
    private final AccessPolicy accessPolicy;

    public MonitoringSessionController(MonitoringSessionService service, SessionTimelineService timelineService,
                                       SessionExportService exportService, AccessPolicy accessPolicy) {
        this.service = service;
        this.timelineService = timelineService;
        this.exportService = exportService;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoringSessionResponse create(@Valid @RequestBody CreateMonitoringSessionRequest request,
                                            @AuthenticationPrincipal UserDetails user,
                                            Authentication authentication) {
        requireRoomManagement(request.roomId(), authentication);
        return service.create(request, user.getUsername());
    }

    @GetMapping
    public List<MonitoringSessionResponse> findAll(@RequestParam(required = false) Long roomId,
                                                   @RequestParam(required = false) MonitoringSessionStatus status,
                                                   Authentication authentication) {
        var access = accessPolicy.forAuthentication(authentication);
        return service.findAll(roomId, status).stream()
                .filter(session -> access.canViewRoom(
                        session.organizationId(), session.labId(), session.roomId()))
                .toList();
    }

    @GetMapping("/{id}")
    public MonitoringSessionResponse findById(@PathVariable Long id, Authentication authentication) {
        return requireSessionAccess(id, authentication);
    }

    @GetMapping("/{id}/timeline")
    public SessionTimelineResponse timeline(@PathVariable Long id,
                                            @RequestParam(required = false) Long sensorId,
                                            Authentication authentication) {
        requireSessionAccess(id, authentication);
        return timelineService.getTimeline(id, sensorId);
    }

    @GetMapping(value = "/{id}/export", produces = "application/zip")
    public ResponseEntity<byte[]> export(@PathVariable Long id, Authentication authentication) {
        requireSessionAccess(id, authentication);
        var export = exportService.export(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .body(export.content());
    }

    @PostMapping("/{id}/start")
    public MonitoringSessionResponse start(@PathVariable Long id, Authentication authentication) {
        requireSessionManagement(id, authentication); return service.start(id);
    }

    @PostMapping("/{id}/complete")
    public MonitoringSessionResponse complete(@PathVariable Long id, Authentication authentication) {
        requireSessionManagement(id, authentication); return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public MonitoringSessionResponse cancel(@PathVariable Long id, Authentication authentication) {
        requireSessionManagement(id, authentication); return service.cancel(id);
    }

    private MonitoringSessionResponse requireSessionAccess(Long id, Authentication authentication) {
        MonitoringSessionResponse session = service.findById(id);
        accessPolicy.forAuthentication(authentication).requireViewRoom(
                session.organizationId(), session.labId(), session.roomId());
        return session;
    }

    private MonitoringSessionResponse requireSessionManagement(Long id, Authentication authentication) {
        MonitoringSessionResponse session = service.findById(id);
        accessPolicy.forAuthentication(authentication).requireManageSession(
                session.organizationId(), session.labId(), session.roomId());
        return session;
    }

    private void requireRoomManagement(Long roomId, Authentication authentication) {
        var room = service.getRoom(roomId);
        accessPolicy.forAuthentication(authentication).requireManageSession(
                room.getLab().getOrganization().getId(), room.getLab().getId(), room.getId());
    }
}
