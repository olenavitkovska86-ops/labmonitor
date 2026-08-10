package com.olena.labmonitor.sensor;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.sensor.dto.CreateSensorRequest;
import com.olena.labmonitor.sensor.dto.SensorResponse;
import com.olena.labmonitor.sensor.dto.UpdateSensorRequest;
import com.olena.labmonitor.sensor.dto.UpdateSensorSafeRangeRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SensorService {

    private final SensorRepository sensorRepository;
    private final RoomService roomService;

    public SensorService(SensorRepository sensorRepository, RoomService roomService) {
        this.sensorRepository = sensorRepository;
        this.roomService = roomService;
    }

    public SensorResponse create(CreateSensorRequest request) {
        Room room = roomService.getExistingRoom(request.roomId());
        requireActiveParents(room, "create a sensor");
        Sensor sensor = new Sensor(room, request.name(), request.type(), request.unit());
        Sensor savedSensor = sensorRepository.saveAndFlush(sensor);

        return SensorResponse.from(savedSensor);
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> findAll(Long roomId, String search) {
        return findSensors(roomId, search).stream()
                .map(SensorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorResponse findById(Long id) {
        return SensorResponse.from(getSensor(id));
    }

    public SensorResponse update(Long id, UpdateSensorRequest request) {
        Sensor sensor = getSensor(id);
        sensor.update(request.name(), request.unit());
        Sensor savedSensor = sensorRepository.saveAndFlush(sensor);

        return SensorResponse.from(savedSensor);
    }

    public SensorResponse updateSafeRange(Long id, UpdateSensorSafeRangeRequest request) {
        Sensor sensor = getSensor(id);
        sensor.updateSafeRange(request.minSafeValue(), request.maxSafeValue());
        Sensor savedSensor = sensorRepository.saveAndFlush(sensor);

        return SensorResponse.from(savedSensor);
    }

    public SensorResponse deactivate(Long id) {
        Sensor sensor = getSensor(id);
        sensor.deactivate();
        Sensor savedSensor = sensorRepository.saveAndFlush(sensor);

        return SensorResponse.from(savedSensor);
    }

    public SensorResponse activate(Long id) {
        Sensor sensor = getSensor(id);
        requireActiveParents(sensor.getRoom(), "activate sensor with id " + id);
        sensor.activate();
        Sensor savedSensor = sensorRepository.saveAndFlush(sensor);

        return SensorResponse.from(savedSensor);
    }

    private List<Sensor> findSensors(Long roomId, String search) {
        boolean hasSearch = hasText(search);

        if (roomId != null && hasSearch) {
            return sensorRepository.searchByRoomIdAndName(roomId, search.trim());
        }

        if (roomId != null) {
            return sensorRepository.findByRoomId(roomId);
        }

        if (hasSearch) {
            return sensorRepository.searchByName(search.trim());
        }

        return sensorRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Sensor getSensor(Long id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor with id " + id + " was not found"));
    }

    public Sensor getExistingSensor(Long id) {
        return getSensor(id);
    }

    public void requireOperationalParents(Sensor sensor, String operation) {
        requireActiveParents(sensor.getRoom(), operation);
    }

    private void requireActiveParents(Room room, String operation) {
        if (!room.getLab().isActive()) {
            throw new InvalidOperationException(
                    "Cannot " + operation + " because lab with id " + room.getLab().getId() + " is inactive"
            );
        }
        if (!room.isActive()) {
            throw new InvalidOperationException(
                    "Cannot " + operation + " because room with id " + room.getId() + " is inactive"
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
