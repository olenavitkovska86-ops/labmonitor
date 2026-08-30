package com.olena.labmonitor.membership;

import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.user.dto.ManagedUserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-access/organizations/{organizationId}")
@PreAuthorize("hasRole('LAB_ADMIN')")
public class TeamAccessController {
    private final TeamAccessService service;

    public TeamAccessController(TeamAccessService service) { this.service = service; }

    @GetMapping
    public List<ManagedUserResponse> findTeam(@PathVariable Long organizationId, Authentication authentication) {
        return service.findTeam(organizationId, authentication.getName());
    }

    @PutMapping("/users/{userId}/scope")
    public ManagedUserResponse updateScope(@PathVariable Long organizationId, @PathVariable Long userId,
                                          @Valid @RequestBody MembershipScopeRequest scope,
                                          Authentication authentication) {
        return service.updateScope(organizationId, userId, scope, authentication.getName());
    }
}
