package com.olena.labmonitor.membership;

import com.olena.labmonitor.membership.dto.MembershipScopeRequest;
import com.olena.labmonitor.user.dto.ManagedUserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import com.olena.labmonitor.security.PermissionCatalog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-access/organizations/{organizationId}")
public class TeamAccessController {
    private final TeamAccessService service;
    private final AccessPolicy accessPolicy;

    public TeamAccessController(TeamAccessService service, AccessPolicy accessPolicy) { this.service = service; this.accessPolicy = accessPolicy; }

    @GetMapping
    public List<ManagedUserResponse> findTeam(@PathVariable Long organizationId, Authentication authentication) {
        accessPolicy.requireOrganization(authentication, PermissionCatalog.TEAM_ACCESS_MANAGE, organizationId);
        return service.findTeam(organizationId, authentication.getName());
    }

    @PutMapping("/users/{userId}/scope")
    public ManagedUserResponse updateScope(@PathVariable Long organizationId, @PathVariable Long userId,
                                          @Valid @RequestBody MembershipScopeRequest scope,
                                          Authentication authentication) {
        accessPolicy.requireOrganization(authentication, PermissionCatalog.TEAM_ACCESS_MANAGE, organizationId);
        return service.updateScope(organizationId, userId, scope, authentication.getName());
    }
}
