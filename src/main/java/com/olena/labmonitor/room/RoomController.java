package com.olena.labmonitor.room;

import com.olena.labmonitor.room.dto.CreateRoomRequest;
import com.olena.labmonitor.room.dto.RoomResponse;
import com.olena.labmonitor.room.dto.UpdateRoomRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.olena.labmonitor.security.AccessPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final AccessPolicy accessPolicy;

    public RoomController(RoomService roomService, AccessPolicy accessPolicy) {
        this.roomService = roomService;
        this.accessPolicy = accessPolicy;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(request);
    }

    @GetMapping
    public List<RoomResponse> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        var access = accessPolicy.forAuthentication(authentication);
        return roomService.findAll(labId, search).stream()
                .filter(room -> access.canViewRoom(room.organizationId(), room.labId(), room.id()))
                .toList();
    }

    @GetMapping("/{id}")
    public RoomResponse findById(@PathVariable Long id, Authentication authentication) {
        var room = roomService.findById(id);
        accessPolicy.forAuthentication(authentication)
                .requireViewRoom(room.organizationId(), room.labId(), room.id());
        return room;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRoomRequest request) {
        return roomService.update(id, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/deactivate")
    public RoomResponse deactivate(@PathVariable Long id) {
        return roomService.deactivate(id);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/activate")
    public RoomResponse activate(@PathVariable Long id) {
        return roomService.activate(id);
    }
}
