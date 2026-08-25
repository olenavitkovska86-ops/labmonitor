package com.olena.labmonitor.session.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {
    @EntityGraph(attributePaths = {"createdBy"})
    List<SessionEvent> findBySessionIdOrderByOccurredAtAscIdAsc(Long sessionId);
}
