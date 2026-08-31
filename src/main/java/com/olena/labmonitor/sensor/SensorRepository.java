package com.olena.labmonitor.sensor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findByDeviceIdAndChannelKey(Long deviceId, String channelKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sensor from Sensor sensor where sensor.device.id = :deviceId and sensor.channelKey = :channelKey")
    Optional<Sensor> findByDeviceIdAndChannelKeyForUpdate(
            @Param("deviceId") Long deviceId,
            @Param("channelKey") String channelKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sensor from Sensor sensor where sensor.id = :id")
    Optional<Sensor> findByIdForUpdate(@Param("id") Long id);
    @EntityGraph(attributePaths = {"room.lab.organization", "device"})
    List<Sensor> findByDeviceId(Long deviceId);

    @Override
    @EntityGraph(attributePaths = {"room.lab.organization", "device"})
    List<Sensor> findAll(Sort sort);

    long countByRoomLabOrganizationIdAndActiveTrueAndStatus(Long organizationId, SensorStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
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

    @EntityGraph(attributePaths = {"room.lab.organization", "device"})
    @Query("""
            select sensor
            from Sensor sensor
            where sensor.room.id = :roomId
            order by sensor.id asc
            """)
    List<Sensor> findByRoomId(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = {"room.lab.organization", "device"})
    @Query("""
            select sensor
            from Sensor sensor
            where lower(sensor.name) like lower(concat('%', :name, '%'))
            order by sensor.id asc
            """)
    List<Sensor> searchByName(@Param("name") String name);

    @EntityGraph(attributePaths = {"room.lab.organization", "device"})
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
