package com.olena.labmonitor.membership;

import com.olena.labmonitor.membership.dto.MembershipResponse;
import com.olena.labmonitor.membership.dto.SaveMembershipRequest;
import com.olena.labmonitor.membership.dto.UpdateMembershipRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class MembershipController {
    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@Valid @RequestBody SaveMembershipRequest request) {
        return membershipService.create(request);
    }

    @GetMapping("/{id}")
    public MembershipResponse findById(@PathVariable Long id) {
        return membershipService.findById(id);
    }

    @PutMapping("/{id}")
    public MembershipResponse update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateMembershipRequest request) {
        return membershipService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        membershipService.delete(id);
    }
}
