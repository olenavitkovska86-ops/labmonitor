package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.dto.MembershipResponse;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipValidator membershipValidator;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public MembershipService(MembershipRepository membershipRepository, MembershipValidator membershipValidator, OrganizationRepository organizationRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.membershipValidator = membershipValidator;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    public MembershipResponse createMembership(Long userId, Long organizationId, String role){
        membershipValidator.validateRole(role);

        if (organizationId == null){
            throw new InvalidOperationException("No organization provided");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Membership membership = new Membership(organization, user, role);
        membershipRepository.save(membership);
        return MembershipResponse.from(membership);
    }


    public MembershipResponse changeOrgRole(Long userId, Long organizationId, String newRole){
        //Fetching a membership
        Membership membership = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membershipValidator.validateRole(newRole);
        // Mutate?, dirty checking
        membership.setRole(newRole);
        return MembershipResponse.from(membership);
    }

    public void deleteMembership(Long userId) {
        List<Membership> memberships = membershipRepository.findByUserId(userId);
        membershipRepository.deleteAll(memberships);
    }
}
