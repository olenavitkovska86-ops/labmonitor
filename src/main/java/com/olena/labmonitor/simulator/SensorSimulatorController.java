package com.olena.labmonitor.simulator;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LAB_ADMIN')")
public class SensorSimulatorController {

    private final SensorReadingSimulator simulator;

    public SensorSimulatorController(SensorReadingSimulator simulator) {
        this.simulator = simulator;
    }

    @GetMapping("/status")
    public SensorReadingSimulator.SimulatorStatus status() {
        return simulator.status();
    }

    @PostMapping("/start")
    public SensorReadingSimulator.SimulatorStatus start(@RequestBody StartSimulatorRequest request) {
        return simulator.start(request.intervalSeconds());
    }

    @PostMapping("/stop")
    public SensorReadingSimulator.SimulatorStatus stop() {
        return simulator.stop();
    }

    public record StartSimulatorRequest(long intervalSeconds) {
    }
}
