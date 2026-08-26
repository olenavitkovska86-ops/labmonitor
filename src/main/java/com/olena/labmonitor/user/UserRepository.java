package com.olena.labmonitor.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
            select user from User user
            where lower(user.firstName) like lower(concat('%', :search, '%'))
               or lower(user.lastName) like lower(concat('%', :search, '%'))
               or lower(user.email) like lower(concat('%', :search, '%'))
            order by user.id
            """)
    List<User> search(@Param("search") String search);

    @Query("""
            select user from User user join user.memberships membership
            where membership.organization.id = :organizationId
            order by user.id
            """)
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
            select user from User user join user.memberships membership
            where membership.organization.id = :organizationId
              and (lower(user.firstName) like lower(concat('%', :search, '%'))
                or lower(user.lastName) like lower(concat('%', :search, '%'))
                or lower(user.email) like lower(concat('%', :search, '%')))
            order by user.id
            """)
    List<User> searchByOrganizationId(@Param("organizationId") Long organizationId,
                                      @Param("search") String search);
}
