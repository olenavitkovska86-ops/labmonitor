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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-sessions")
public class MonitoringSessionController {

    private final MonitoringSessionService service;
    private final SessionTimelineService timelineService;
    private final SessionExportService exportService;

    public MonitoringSessionController(MonitoringSessionService service, SessionTimelineService timelineService,
                                       SessionExportService exportService) {
        this.service = service;
        this.timelineService = timelineService;
        this.exportService = exportService;
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

    @GetMapping("/{id}/timeline")
    public SessionTimelineResponse timeline(@PathVariable Long id,
                                            @RequestParam(required = false) Long sensorId) {
        return timelineService.getTimeline(id, sensorId);
    }

    @GetMapping(value = "/{id}/export", produces = "application/zip")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        var export = exportService.export(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .body(export.content());
    }

    @PostMapping("/{id}/start")
    public MonitoringSessionResponse start(@PathVariable Long id) { return service.start(id); }

    @PostMapping("/{id}/complete")
    public MonitoringSessionResponse complete(@PathVariable Long id) { return service.complete(id); }

    @PostMapping("/{id}/cancel")
    public MonitoringSessionResponse cancel(@PathVariable Long id) { return service.cancel(id); }
}
