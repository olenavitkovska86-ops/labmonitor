package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomService;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SensorReadingExportService {

    private static final String HEADER = "measured_at,received_at,room_id,room,sensor_id,sensor,"
            + "sensor_type,value,unit,safe_min,safe_max,status\n";

    private final SensorReadingRepository repository;
    private final RoomService roomService;
    private final SensorService sensorService;
    private final MonitoringProperties properties;

    public SensorReadingExportService(
            SensorReadingRepository repository,
            RoomService roomService,
            SensorService sensorService,
            MonitoringProperties properties
    ) {
        this.repository = repository;
        this.roomService = roomService;
        this.sensorService = sensorService;
        this.properties = properties;
    }

    public CsvExport export(Long roomId, Long sensorId, LocalDateTime from, LocalDateTime to) {
        validatePeriod(from, to);
        Room room = roomService.getExistingRoom(roomId);
        if (sensorId != null) {
            Sensor sensor = sensorService.getExistingSensor(sensorId);
            if (!sensor.getRoom().getId().equals(roomId)) {
                throw new IllegalArgumentException("Sensor " + sensorId + " does not belong to room " + roomId);
            }
        }

        int maxRows = properties.getExports().getMaxRows();
        List<SensorReading> readings = repository.findForExport(
                roomId, sensorId, from, to, PageRequest.of(0, maxRows + 1)
        );
        if (readings.size() > maxRows) {
            throw new IllegalArgumentException(
                    "Export contains more than " + maxRows + " readings; select a shorter period"
            );
        }

        StringBuilder csv = new StringBuilder(HEADER.length() + readings.size() * 140);
        csv.append('\uFEFF').append(HEADER);
        readings.forEach(reading -> appendReading(csv, reading));
        String scope = sensorId == null ? "room-" + roomId : "sensor-" + sensorId;
        return new CsvExport(scope + "-readings.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    public Room getRoom(Long roomId) {
        return roomService.getExistingRoom(roomId);
    }

    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) throw new IllegalArgumentException("Export start and end time are required");
        if (!from.isBefore(to)) throw new IllegalArgumentException("Export start time must be before end time");
        if (Duration.between(from, to).compareTo(properties.getExports().getMaxPeriod()) > 0) {
            throw new IllegalArgumentException("Export period cannot exceed "
                    + properties.getExports().getMaxPeriod().toDays() + " days");
        }
    }

    private void appendReading(StringBuilder csv, SensorReading reading) {
        Sensor sensor = reading.getSensor();
        Room room = reading.getRoom();
        append(csv, reading.getMeasuredAt());
        append(csv, reading.getCreatedAt());
        append(csv, room.getId());
        append(csv, room.getName());
        append(csv, sensor.getId());
        append(csv, sensor.getName());
        append(csv, sensor.getType());
        append(csv, reading.getValue());
        append(csv, sensor.getUnit());
        append(csv, reading.getSafeMin());
        append(csv, reading.getSafeMax());
        csv.append(reading.getStatus()).append('\n');
    }

    private void append(StringBuilder csv, Object value) {
        String text = value == null ? "" : value.toString();
        if (!(value instanceof Number) && !text.isEmpty()
                && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        csv.append('"').append(text.replace("\"", "\"\"")).append("\",");
    }

    public record CsvExport(String filename, byte[] content) {
    }
}
