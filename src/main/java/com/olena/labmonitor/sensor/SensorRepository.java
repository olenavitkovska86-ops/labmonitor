package com.olena.labmonitor.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findByDeviceIdAndChannelKey(Long deviceId, String channelKey);
    List<Sensor> findByDeviceId(Long deviceId);

    long countByRoomLabOrganizationIdAndActiveTrueAndStatus(Long organizationId, SensorStatus status);

    @Query("""
            select sensor
            from Sensor sensor
            where sensor.active = true
              and sensor.room.active = true
              and sensor.room.lab.active = true
              and sensor.status in (com.olena.labmonitor.sensor.SensorStatus.ONLINE,
                                    com.olena.labmonitor.sensor.SensorStatus.OFFLINE)
              and ((sensor.lastSeenAt is not null and sensor.lastSeenAt < :cutoff)
                   or (sensor.lastSeenAt is null and sensor.createdAt < :cutoff))
            order by sensor.id asc
            """)
    List<Sensor> findSensorsMissingSince(@Param("cutoff") LocalDateTime cutoff);

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
