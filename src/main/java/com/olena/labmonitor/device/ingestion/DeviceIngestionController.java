package com.olena.labmonitor.device.ingestion;

import com.olena.labmonitor.device.ingestion.dto.*;
import com.olena.labmonitor.device.security.DevicePrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
public class DeviceIngestionController {
    private final DeviceIngestionService ingestionService;

    public DeviceIngestionController(DeviceIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/readings")
    public DeviceReadingResponse ingest(@Valid @RequestBody DeviceReadingRequest request,
                                        Authentication authentication) {
        return ingestionService.ingest((DevicePrincipal) authentication.getPrincipal(), request);
    }
}
