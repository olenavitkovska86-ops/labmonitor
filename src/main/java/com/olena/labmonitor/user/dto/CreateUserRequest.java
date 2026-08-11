package com.olena.labmonitor.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotNull(message = "Email is required")
        @Email
        String email,

        @NotNull(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotNull(message = "Last name is Required")
        @Size(max = 100)
        String lastName,

        @NotNull(message = "Password is required")
        String password
) {
}
