package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserValidator {
    private static final Set<String> VALID_ROLES = Set.of("SUPER_ADMIN", "LAB_ADMIN", "LIMITED_EMPLOYEE");
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public UserValidator(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    //
    public void validateEmail(String email){
        if (userRepository.findByEmail(email).isPresent()){
            throw new InvalidOperationException("Email is already registered.");
        }

        if (!email.matches(EMAIL_PATTERN)) {
            throw new InvalidOperationException("Email format is invalid.");
        }
    }

    public void validateRole(String role){
        if (!VALID_ROLES.contains(role)){
            throw new InvalidOperationException("Invalid role: " + role);
        }
    }

    public Organization validateAndGetOrganization(Long organization){
        if (organization == null){
            throw new InvalidOperationException("Organization ID is required for this role");
        }
        return organizationRepository.findById(organization)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
