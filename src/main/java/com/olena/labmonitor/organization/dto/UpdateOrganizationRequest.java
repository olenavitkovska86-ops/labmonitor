package com.olena.labmonitor.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Organization name must not be longer than 150 characters")
        String name,

        @Size(max = 500, message = "Organization description must not be longer than 500 characters")
        String description
) {
}
