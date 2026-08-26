package com.olena.labmonitor.lab;

import com.olena.labmonitor.lab.dto.CreateLabRequest;
import com.olena.labmonitor.lab.dto.LabResponse;
import com.olena.labmonitor.lab.dto.UpdateLabRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
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
    private final AccessPolicy accessPolicy;

    public LabController(LabService labService, AccessPolicy accessPolicy) {
        this.labService = labService;
        this.accessPolicy = accessPolicy;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabResponse create(@Valid @RequestBody CreateLabRequest request) {
        return labService.create(request);
    }

    @GetMapping
    public List<LabResponse> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        var access = accessPolicy.forAuthentication(authentication);
        return labService.findAll(organizationId, search).stream()
                .filter(lab -> access.canViewLab(lab.organizationId(), lab.id()))
                .toList();
    }

    @GetMapping("/{id}")
    public LabResponse findById(@PathVariable Long id, Authentication authentication) {
        var lab = labService.findById(id);
        accessPolicy.forAuthentication(authentication).requireViewLab(lab.organizationId(), lab.id());
        return lab;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public LabResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLabRequest request) {
        return labService.update(id, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/deactivate")
    public LabResponse deactivate(@PathVariable Long id) {
        return labService.deactivate(id);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/activate")
    public LabResponse activate(@PathVariable Long id) {
        return labService.activate(id);
    }
}
