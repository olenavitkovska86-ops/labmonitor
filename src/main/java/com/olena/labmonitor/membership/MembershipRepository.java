package com.olena.labmonitor.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    @EntityGraph(attributePaths = {"organization", "user", "accessibleLabs", "accessibleRooms"})
    Optional<Membership> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    @EntityGraph(attributePaths = {"organization", "user", "accessibleLabs", "accessibleRooms"})
    List<Membership> findByUserId(Long userId);

    @Override
    @EntityGraph(attributePaths = {"organization", "user", "accessibleLabs", "accessibleRooms"})
    Optional<Membership> findById(Long id);
}
