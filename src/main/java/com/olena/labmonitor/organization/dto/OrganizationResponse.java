package com.olena.labmonitor.organization.dto;

import com.olena.labmonitor.organization.Organization;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}
