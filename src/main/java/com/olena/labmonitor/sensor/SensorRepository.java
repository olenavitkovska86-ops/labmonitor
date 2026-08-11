package com.olena.labmonitor.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    @Query("""
            select sensor
            from Sensor sensor
            where sensor.room.id = :roomId
            order by sensor.id asc
            """)
    List<Sensor> findByRoomId(@Param("roomId") Long roomId);

    @Query("""
            select sensor
            from Sensor sensor
            where lower(sensor.name) like lower(concat('%', :name, '%'))
            order by sensor.id asc
            """)
    List<Sensor> searchByName(@Param("name") String name);

    @Query("""
            select sensor
            from Sensor sensor
            where sensor.room.id = :roomId
              and lower(sensor.name) like lower(concat('%', :name, '%'))
            order by sensor.id asc
            """)
    List<Sensor> searchByRoomIdAndName(
            @Param("roomId") Long roomId,
            @Param("name") String name
    );
}
