package com.olena.labmonitor.room;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabService;
import com.olena.labmonitor.room.dto.CreateRoomRequest;
import com.olena.labmonitor.room.dto.RoomResponse;
import com.olena.labmonitor.room.dto.UpdateRoomRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final LabService labService;

    public RoomService(RoomRepository roomRepository, LabService labService) {
        this.roomRepository = roomRepository;
        this.labService = labService;
    }

    public RoomResponse create(CreateRoomRequest request) {
        Lab lab = labService.getExistingLab(request.labId());
        requireActiveLab(lab, "create a room");
        Room room = new Room(lab, request.name(), request.type(), request.floor(), request.area());
        Room savedRoom = roomRepository.saveAndFlush(room);

        return RoomResponse.from(savedRoom);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> findAll(Long labId, String search) {
        List<Room> rooms = findRooms(labId, search);

        return rooms.stream()
                .map(RoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse findById(Long id) {
        Room room = getRoom(id);

        return RoomResponse.from(room);
    }

    public RoomResponse update(Long id, UpdateRoomRequest request) {
        Room room = getRoom(id);
        room.update(request.name(), request.type(), request.floor(), request.area());
        Room savedRoom = roomRepository.saveAndFlush(room);

        return RoomResponse.from(savedRoom);
    }

    public RoomResponse deactivate(Long id) {
        Room room = getRoom(id);
        room.deactivate();
        Room savedRoom = roomRepository.saveAndFlush(room);

        return RoomResponse.from(savedRoom);
    }

    public RoomResponse activate(Long id) {
        Room room = getRoom(id);
        requireActiveLab(room.getLab(), "activate room with id " + id);
        room.activate();
        Room savedRoom = roomRepository.saveAndFlush(room);

        return RoomResponse.from(savedRoom);
    }

    private List<Room> findRooms(Long labId, String search) {
        boolean hasSearch = hasText(search);

        if (labId != null && hasSearch) {
            return roomRepository.searchByLabIdAndName(labId, search.trim());
        }

        if (labId != null) {
            return roomRepository.findByLabId(labId);
        }

        if (hasSearch) {
            return roomRepository.searchByName(search.trim());
        }

        return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room with id " + id + " was not found"));
    }

    public Room getExistingRoom(Long id) {
        return getRoom(id);
    }

    private void requireActiveLab(Lab lab, String operation) {
        if (!lab.isActive()) {
            throw new InvalidOperationException(
                    "Cannot " + operation + " because lab with id " + lab.getId() + " is inactive"
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
