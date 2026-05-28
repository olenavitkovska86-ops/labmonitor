package com.olena.labmonitor.room.dto;

import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        Long labId,
        Long organizationId,
        String name,
        RoomType type,
        Integer floor,
        BigDecimal area,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getLab().getId(),
                room.getLab().getOrganization().getId(),
                room.getName(),
                room.getType(),
                room.getFloor(),
                room.getArea(),
                room.isActive(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
