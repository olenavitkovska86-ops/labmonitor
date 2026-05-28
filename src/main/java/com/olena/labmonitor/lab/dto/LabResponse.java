package com.olena.labmonitor.lab.dto;

import com.olena.labmonitor.lab.Lab;

import java.time.LocalDateTime;

public record LabResponse(
        Long id,
        Long organizationId,
        String name,
        String location,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static LabResponse from(Lab lab) {
        return new LabResponse(
                lab.getId(),
                lab.getOrganization().getId(),
                lab.getName(),
                lab.getLocation(),
                lab.getDescription(),
                lab.isActive(),
                lab.getCreatedAt(),
                lab.getUpdatedAt()
        );
    }
}
