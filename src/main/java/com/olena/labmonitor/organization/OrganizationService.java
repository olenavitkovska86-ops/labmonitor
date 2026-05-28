package com.olena.labmonitor.organization;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.organization.dto.CreateOrganizationRequest;
import com.olena.labmonitor.organization.dto.OrganizationResponse;
import com.olena.labmonitor.organization.dto.UpdateOrganizationRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrganizationResponse create(CreateOrganizationRequest request) {
        Organization organization = new Organization(request.name(), request.description());
        Organization savedOrganization = organizationRepository.saveAndFlush(organization);

        return OrganizationResponse.from(savedOrganization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> findAll(String search) {
        List<Organization> organizations = hasText(search)
                ? organizationRepository.findByNameContainingIgnoreCaseOrderByIdAsc(search.trim())
                : organizationRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        return organizations
                .stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id) {
        Organization organization = getOrganization(id);

        return OrganizationResponse.from(organization);
    }

    public OrganizationResponse update(Long id, UpdateOrganizationRequest request) {
        Organization organization = getOrganization(id);
        organization.update(request.name(), request.description());
        Organization savedOrganization = organizationRepository.saveAndFlush(organization);

        return OrganizationResponse.from(savedOrganization);
    }

    public void delete(Long id) {
        Organization organization = getOrganization(id);
        organizationRepository.delete(organization);
    }

    private Organization getOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with id " + id + " was not found"));
    }

    public Organization getExistingOrganization(Long id) {
        return getOrganization(id);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
