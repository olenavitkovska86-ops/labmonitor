package com.olena.labmonitor.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserIdAndOrganizationId(Long userId, Long organizationId);
}
