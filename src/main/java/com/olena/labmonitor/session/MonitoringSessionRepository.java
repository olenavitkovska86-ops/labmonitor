package com.olena.labmonitor.session;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    boolean existsByRoomIdAndStatus(Long roomId, MonitoringSessionStatus status);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByRoomId(Long roomId, Sort sort);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByStatus(MonitoringSessionStatus status, Sort sort);

    @EntityGraph(attributePaths = {"room.lab.organization", "createdBy"})
    List<MonitoringSession> findByRoomIdAndStatus(Long roomId, MonitoringSessionStatus status, Sort sort);
}
