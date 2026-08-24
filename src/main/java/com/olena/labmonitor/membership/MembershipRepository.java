package com.olena.labmonitor.membership;

import com.olena.labmonitor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Override
    Optional<Membership> findById(Long aLong);

    Optional<Membership> findByUserIdAndOrganizationId(Long userId, Long orgId);

    Long user(User user);
}
