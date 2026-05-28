package com.olena.labmonitor.lab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabRepository extends JpaRepository<Lab, Long> {

    List<Lab> findByOrganizationIdOrderByIdAsc(Long organizationId);

    List<Lab> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Lab> findByOrganizationIdAndNameContainingIgnoreCaseOrderByIdAsc(Long organizationId, String name);
}
