package com.olena.labmonitor.sensor.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    @Query("""
            select reading
            from SensorReading reading
            where reading.sensor.id = :sensorId
            order by reading.measuredAt desc, reading.id desc
            """)
    List<SensorReading> findHistoryBySensorId(@Param("sensorId") Long sensorId);

    Optional<SensorReading> findFirstBySensorIdOrderByMeasuredAtDescIdDesc(Long sensorId);
}
