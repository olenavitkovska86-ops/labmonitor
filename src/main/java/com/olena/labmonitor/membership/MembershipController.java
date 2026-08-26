package com.olena.labmonitor.membership;

import com.olena.labmonitor.membership.dto.ChangeRoleRequest;
import com.olena.labmonitor.membership.dto.MembershipResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {
    private final MembershipService membershipService;
    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/organizations/{organizationId}/users/{userId}/role")
    public ResponseEntity<MembershipResponse> changeOrgRole(@PathVariable Long organizationId,
                                                            @PathVariable Long userId,
                                                            @RequestBody ChangeRoleRequest request){
        MembershipResponse updatedRole = membershipService.changeOrgRole(userId, organizationId, request.role());
        return ResponseEntity.ok(updatedRole);
    }
}
