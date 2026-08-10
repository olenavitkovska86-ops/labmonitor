package com.olena.labmonitor.lab;

import com.olena.labmonitor.lab.dto.CreateLabRequest;
import com.olena.labmonitor.lab.dto.LabResponse;
import com.olena.labmonitor.lab.dto.UpdateLabRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabResponse create(@Valid @RequestBody CreateLabRequest request) {
        return labService.create(request);
    }

    @GetMapping
    public List<LabResponse> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String search
    ) {
        return labService.findAll(organizationId, search);
    }

    @GetMapping("/{id}")
    public LabResponse findById(@PathVariable Long id) {
        return labService.findById(id);
    }

    @PutMapping("/{id}")
    public LabResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLabRequest request) {
        return labService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public LabResponse deactivate(@PathVariable Long id) {
        return labService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public LabResponse activate(@PathVariable Long id) {
        return labService.activate(id);
    }
}
