package com.olena.labmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    @Override
    @EntityGraph(attributePaths = {"room.lab.organization", "sensor", "acknowledgedByUser", "resolvedByUser", "reopenedByUser"})
    List<Alert> findAll(Specification<Alert> specification, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"room.lab.organization", "sensor", "acknowledgedByUser", "resolvedByUser", "reopenedByUser"})
    Optional<Alert> findById(Long id);

    boolean existsBySensorIdAndTypeAndStatusIn(
            Long sensorId,
            AlertType type,
            Collection<AlertStatus> statuses
    );

    Optional<Alert> findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(
            Long sensorId,
            AlertType type,
            Collection<AlertStatus> statuses
    );

    Optional<Alert> findFirstBySensorIdAndTypeAndStatusIn(
            Long sensorId,
            AlertType type,
            Collection<AlertStatus> statuses
    );

    long countByStatusIn(Collection<AlertStatus> statuses);

    @EntityGraph(attributePaths = {"room.lab.organization", "sensor"})
    @Query("""
            select alert
            from Alert alert
            where alert.room.lab.organization.id = :organizationId
              and alert.status in :statuses
            order by alert.createdAt asc
            """)
    List<Alert> findByOrganizationIdAndStatusIn(
            @Param("organizationId") Long organizationId,
            @Param("statuses") Collection<AlertStatus> statuses
    );

    @EntityGraph(attributePaths = {"room.lab.organization", "sensor"})
    @Query("""
            select alert
            from Alert alert
            where alert.room.lab.organization.id = :organizationId
              and alert.createdAt between :from and :to
            order by alert.createdAt asc
            """)
    List<Alert> findByOrganizationIdAndCreatedAtBetween(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
