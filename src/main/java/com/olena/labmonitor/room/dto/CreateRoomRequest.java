package com.olena.labmonitor.room.dto;

import com.olena.labmonitor.room.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRoomRequest(
        @NotNull(message = "Lab id is required")
        Long labId,

        @NotBlank(message = "Room name is required")
        @Size(max = 100, message = "Room name must not be longer than 100 characters")
        String name,

        @NotNull(message = "Room type is required")
        RoomType type,

        Integer floor,

        @DecimalMin(value = "0.0", inclusive = false, message = "Room area must be greater than 0")
        BigDecimal area
) {
}
