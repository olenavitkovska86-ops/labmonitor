package com.olena.labmonitor.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
        select user
        from User user
        where lower(user.firstName) like lower(concat('%', :name, '%'))
        or lower(user.lastName) like lower(concat('%', :name, '%'))
        or lower(user.email) like lower(concat('%', :name, '%'))
        order by user.id asc
""")
    List<User> searchUserByName(@Param("name") String name);

    @Query("""
        select user
        from User user
        join user.memberships m
        where m.organization.id = :organizationId
        order by user.id asc
""")
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
        select user
        from User user
        join user.memberships m
        where m.organization.id = :organizationId
        and (lower(user.firstName) like lower(concat('%', :name, '%'))
        or lower(user.lastName) like lower(concat('%', :name, '%'))
        or lower(user.email) like lower(concat('%', :name, '%')))
        order by user.id asc
""")
    List<User> searchByOrganizationIdAndName(@Param("organizationId") Long organizationId,
                                             @Param("name") String name);
}
