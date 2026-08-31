package com.olena.labmonitor.device;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    @EntityGraph(attributePaths = "room.lab.organization")
    List<Device> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "room.lab.organization")
    List<Device> findByRoomLabOrganizationIdOrderByNameAsc(Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from Device device where device.id = :id")
    Optional<Device> findByIdForUpdate(@Param("id") Long id);
}
