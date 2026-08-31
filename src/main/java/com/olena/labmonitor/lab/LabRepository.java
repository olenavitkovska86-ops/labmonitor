package com.olena.labmonitor.lab;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Sort;

public interface LabRepository extends JpaRepository<Lab, Long> {

    @Override
    @EntityGraph(attributePaths = "organization")
    List<Lab> findAll(Sort sort);

    @EntityGraph(attributePaths = "organization")
    @Query("""
            select lab
            from Lab lab
            where lab.organization.id = :organizationId
            order by lab.id asc
            """)
    List<Lab> findByOrganizationId(@Param("organizationId") Long organizationId);

    @EntityGraph(attributePaths = "organization")
    @Query("""
            select lab
            from Lab lab
            where lower(lab.name) like lower(concat('%', :name, '%'))
            order by lab.id asc
            """)
    List<Lab> searchByName(@Param("name") String name);

    @EntityGraph(attributePaths = "organization")
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
