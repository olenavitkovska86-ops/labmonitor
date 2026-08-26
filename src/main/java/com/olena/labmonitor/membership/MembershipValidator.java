package com.olena.labmonitor.membership;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MembershipValidator {

    private static final Set<String> VALID_ROLES = Set.of("LAB_ADMIN", "LIMITED_EMPLOYEE");

    public void validateRole(String role){
        if (!VALID_ROLES.contains(role)){
            throw new InvalidOperationException("Invalid role: " + role);
        }
    }



}
