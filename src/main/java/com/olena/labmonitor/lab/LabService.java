package com.olena.labmonitor.lab;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.lab.dto.CreateLabRequest;
import com.olena.labmonitor.lab.dto.LabResponse;
import com.olena.labmonitor.lab.dto.UpdateLabRequest;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LabService {

    private final LabRepository labRepository;
    private final OrganizationService organizationService;

    public LabService(LabRepository labRepository, OrganizationService organizationService) {
        this.labRepository = labRepository;
        this.organizationService = organizationService;
    }

    public LabResponse create(CreateLabRequest request) {
        Organization organization = organizationService.getExistingOrganization(request.organizationId());
        Lab lab = new Lab(organization, request.name(), request.location(), request.description());
        Lab savedLab = labRepository.saveAndFlush(lab);

        return LabResponse.from(savedLab);
    }

    @Transactional(readOnly = true)
    public List<LabResponse> findAll(Long organizationId, String search) {
        List<Lab> labs = findLabs(organizationId, search);

        return labs.stream()
                .map(LabResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabResponse findById(Long id) {
        Lab lab = getLab(id);

        return LabResponse.from(lab);
    }

    public LabResponse update(Long id, UpdateLabRequest request) {
        Lab lab = getLab(id);
        lab.update(request.name(), request.location(), request.description());
        Lab savedLab = labRepository.saveAndFlush(lab);

        return LabResponse.from(savedLab);
    }

    public LabResponse deactivate(Long id) {
        Lab lab = getLab(id);
        lab.deactivate();
        Lab savedLab = labRepository.saveAndFlush(lab);

        return LabResponse.from(savedLab);
    }

    public LabResponse activate(Long id) {
        Lab lab = getLab(id);
        lab.activate();
        Lab savedLab = labRepository.saveAndFlush(lab);

        return LabResponse.from(savedLab);
    }

    private List<Lab> findLabs(Long organizationId, String search) {
        boolean hasSearch = hasText(search);

        if (organizationId != null && hasSearch) {
            return labRepository.searchByOrganizationIdAndName(
                    organizationId,
                    search.trim()
            );
        }

        if (organizationId != null) {
            return labRepository.findByOrganizationId(organizationId);
        }

        if (hasSearch) {
            return labRepository.searchByName(search.trim());
        }

        return labRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Lab getLab(Long id) {
        return labRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab with id " + id + " was not found"));
    }

    public Lab getExistingLab(Long id) {
        return getLab(id);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
