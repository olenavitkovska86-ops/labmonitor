package com.olena.labmonitor.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query("""
            select organization
            from Organization organization
            where lower(organization.name) like lower(concat('%', :name, '%'))
            order by organization.id asc
            """)
    List<Organization> searchByName(@Param("name") String name);
}
