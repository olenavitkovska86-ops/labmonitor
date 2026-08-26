package com.olena.labmonitor.organization;

import com.olena.labmonitor.organization.dto.CreateOrganizationRequest;
import com.olena.labmonitor.organization.dto.OrganizationResponse;
import com.olena.labmonitor.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final AccessPolicy accessPolicy;

    public OrganizationController(OrganizationService organizationService, AccessPolicy accessPolicy) {
        this.organizationService = organizationService;
        this.accessPolicy = accessPolicy;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(request);
    }

    @GetMapping
    public List<OrganizationResponse> findAll(@RequestParam(required = false) String search,
                                              Authentication authentication) {
        var access = accessPolicy.forAuthentication(authentication);
        return organizationService.findAll(search).stream()
                .filter(organization -> access.canViewOrganization(organization.id()))
                .toList();
    }

    @GetMapping("/{id}")
    public OrganizationResponse findById(@PathVariable Long id, Authentication authentication) {
        var organization = organizationService.findById(id);
        accessPolicy.forAuthentication(authentication).requireViewOrganization(organization.id());
        return organization;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public OrganizationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return organizationService.update(id, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        organizationService.delete(id);
    }
}
