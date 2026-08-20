package com.olena.labmonitor.alert.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
}
