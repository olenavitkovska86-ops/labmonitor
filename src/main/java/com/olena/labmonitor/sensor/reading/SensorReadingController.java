package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.sensor.reading.dto.CreateSensorReadingRequest;
import com.olena.labmonitor.sensor.reading.dto.SensorReadingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import com.olena.labmonitor.sensor.SensorService;

import java.util.List;

@RestController
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;
    private final SensorReadingExportService exportService;
    private final SensorService sensorService;
    private final AccessPolicy accessPolicy;

    public SensorReadingController(
            SensorReadingService sensorReadingService,
            SensorReadingExportService exportService,
            SensorService sensorService,
            AccessPolicy accessPolicy
    ) {
        this.sensorReadingService = sensorReadingService;
        this.exportService = exportService;
        this.sensorService = sensorService;
        this.accessPolicy = accessPolicy;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/api/sensor-readings")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingResponse create(@Valid @RequestBody CreateSensorReadingRequest request) {
        return sensorReadingService.create(request);
    }

    @GetMapping("/api/sensors/{sensorId}/current-reading")
    public ResponseEntity<SensorReadingResponse> findCurrent(@PathVariable Long sensorId,
                                                             Authentication authentication) {
        requireSensorAccess(sensorId, authentication);
        return sensorReadingService.findCurrent(sensorId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/sensors/{sensorId}/readings")
    public List<SensorReadingResponse> findHistory(
            @PathVariable Long sensorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        requireSensorAccess(sensorId, authentication);
        return sensorReadingService.findHistory(sensorId, from, to, limit);
    }

    @GetMapping(value = "/api/sensor-readings/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam Long roomId,
            @RequestParam(required = false) Long sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Authentication authentication
    ) {
        var roomEntity = exportService.getRoom(roomId);
        accessPolicy.forAuthentication(authentication).requireViewRoom(
                roomEntity.getLab().getOrganization().getId(), roomEntity.getLab().getId(), roomEntity.getId());
        if (sensorId != null) requireSensorAccess(sensorId, authentication);
        var export = exportService.export(roomId, sensorId, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .body(export.content());
    }

    private void requireSensorAccess(Long sensorId, Authentication authentication) {
        var sensor = sensorService.findById(sensorId);
        accessPolicy.forAuthentication(authentication).requireViewRoom(
                sensor.organizationId(), sensor.labId(), sensor.roomId());
    }
}
