package com.olena.labmonitor.sensor.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Optional<SensorReading> findBySourceDeviceIdAndMessageId(Long sourceDeviceId, String messageId);

    Optional<SensorReading> findFirstBySensorIdOrderByMeasuredAtDescIdDesc(Long sensorId);

    List<SensorReading> findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDescIdDesc(
            Long sensorId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            select reading
            from SensorReading reading
            join fetch reading.sensor sensor
            join fetch reading.room room
            where room.id = :roomId
              and (:sensorId is null or sensor.id = :sensorId)
              and reading.measuredAt between :from and :to
            order by reading.measuredAt asc, reading.id asc
            """)
    List<SensorReading> findForExport(
            @Param("roomId") Long roomId,
            @Param("sensorId") Long sensorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query(value = """
            select reading.*
            from sensor_readings reading
            join (
                select candidate.id,
                       row_number() over (
                           partition by candidate.sensor_id
                           order by candidate.measured_at asc, candidate.id asc
                       ) as sample_row_number,
                       count(*) over (partition by candidate.sensor_id) as total_rows
                from sensor_readings candidate
                where candidate.room_id = :roomId
                  and candidate.sensor_id = coalesce(:sensorId, candidate.sensor_id)
                  and candidate.measured_at between :from and :to
            ) ranked on ranked.id = reading.id
            where ranked.total_rows <= :maxPointsPerSensor
               or ranked.sample_row_number = 1
               or floor((ranked.sample_row_number - 1) * (:maxPointsPerSensor - 1)
                    / nullif(ranked.total_rows - 1, 0))
                    > floor((ranked.sample_row_number - 2) * (:maxPointsPerSensor - 1)
                    / nullif(ranked.total_rows - 1, 0))
            order by reading.measured_at asc, reading.id asc
            """, nativeQuery = true)
    List<SensorReading> findSampledForTimeline(
            @Param("roomId") Long roomId,
            @Param("sensorId") Long sensorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("maxPointsPerSensor") int maxPointsPerSensor
    );

    @Query("""
            select count(reading)
            from SensorReading reading
            where reading.room.id = :roomId
              and (:sensorId is null or reading.sensor.id = :sensorId)
              and reading.measuredAt between :from and :to
            """)
    long countForTimeline(
            @Param("roomId") Long roomId,
            @Param("sensorId") Long sensorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
