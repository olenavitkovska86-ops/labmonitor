package com.olena.labmonitor.session.event;

import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_events")
public class SessionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MonitoringSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionEventCategory category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SessionEvent() {
    }

    public SessionEvent(MonitoringSession session, SessionEventCategory category, String title,
                        String description, LocalDateTime occurredAt, User createdBy) {
        this.session = session;
        this.category = category;
        this.title = title;
        this.description = description;
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public MonitoringSession getSession() { return session; }
    public SessionEventCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
