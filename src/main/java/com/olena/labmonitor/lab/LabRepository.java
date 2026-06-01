package com.olena.labmonitor.lab;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LabRepository extends JpaRepository<Lab, Long> {

    @Query("""
            select lab
            from Lab lab
            where lab.organization.id = :organizationId
            order by lab.id asc
            """)
    List<Lab> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
            select lab
            from Lab lab
            where lower(lab.name) like lower(concat('%', :name, '%'))
            order by lab.id asc
            """)
    List<Lab> searchByName(@Param("name") String name);

    @Query("""
            select lab
            from Lab lab
            where lab.organization.id = :organizationId
              and lower(lab.name) like lower(concat('%', :name, '%'))
            order by lab.id asc
            """)
    List<Lab> searchByOrganizationIdAndName(
            @Param("organizationId") Long organizationId,
            @Param("name") String name
    );
}
