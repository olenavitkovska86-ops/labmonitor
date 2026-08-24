package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.dto.MembershipResponse;
import com.olena.labmonitor.user.UserValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserValidator userValidator;

    public MembershipService(MembershipRepository membershipRepository, UserValidator userValidator) {
        this.membershipRepository = membershipRepository;
        this.userValidator = userValidator;
    }

    public MembershipResponse changeOrgRole(Long userId, Long organizationId, String newRole){
        Membership membership = membershipRepository.findByUserIdAndOrganizationId(user, o)
    }
}
