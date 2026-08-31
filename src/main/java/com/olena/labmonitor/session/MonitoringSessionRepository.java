package com.olena.labmonitor.session;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    boolean existsByRoomIdAndStatus(Long roomId, MonitoringSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    @Query("select session from MonitoringSession session where session.id = :id")
    Optional<MonitoringSession> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByRoomId(Long roomId, Sort sort);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByStatus(MonitoringSessionStatus status, Sort sort);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByRoomIdAndStatus(Long roomId, MonitoringSessionStatus status, Sort sort);
}
