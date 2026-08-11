package com.olena.labmonitor.user.dto;

import com.olena.labmonitor.user.User;

import java.time.LocalDateTime;

public record UserResponce(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String status,
        String globalRole,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponce from(User user){
        return new UserResponce(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getGlobalRole(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
