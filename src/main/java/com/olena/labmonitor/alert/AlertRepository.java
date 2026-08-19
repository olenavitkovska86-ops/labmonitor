package com.olena.labmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    @Override
    @EntityGraph(attributePaths = {"room.lab.organization", "sensor"})
    List<Alert> findAll(Specification<Alert> specification, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"room.lab.organization", "sensor"})
    Optional<Alert> findById(Long id);

    boolean existsBySensorIdAndTypeAndStatusIn(
            Long sensorId,
            AlertType type,
            Collection<AlertStatus> statuses
    );
}
