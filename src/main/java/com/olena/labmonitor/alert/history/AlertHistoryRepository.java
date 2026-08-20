package com.olena.labmonitor.alert.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    @EntityGraph(attributePaths = "performedByUser")
    List<AlertHistory> findByAlertIdOrderByCreatedAtAsc(Long alertId);
}
