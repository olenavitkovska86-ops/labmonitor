package com.olena.labmonitor.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLabRequest(
        @NotNull(message = "Organization id is required")
        Long organizationId,

        @NotBlank(message = "Lab name is required")
        @Size(max = 150, message = "Lab name must not be longer than 150 characters")
        String name,

        @Size(max = 255, message = "Lab location must not be longer than 255 characters")
        String location,

        @Size(max = 500, message = "Lab description must not be longer than 500 characters")
        String description
) {
}
