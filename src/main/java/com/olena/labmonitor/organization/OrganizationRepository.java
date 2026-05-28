package com.olena.labmonitor.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByNameContainingIgnoreCaseOrderByIdAsc(String name);
}
