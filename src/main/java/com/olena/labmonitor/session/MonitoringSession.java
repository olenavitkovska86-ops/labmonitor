package com.olena.labmonitor.session;

import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitoring_sessions")
public class MonitoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MonitoringSessionStatus status = MonitoringSessionStatus.PLANNED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MonitoringSession() {
    }

    public MonitoringSession(Room room, String name, String description, User createdBy) {
        this.room = room;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    public void start(LocalDateTime time) {
        status = MonitoringSessionStatus.ACTIVE;
        startedAt = time;
    }

    public void complete(LocalDateTime time) {
        status = MonitoringSessionStatus.COMPLETED;
        endedAt = time;
    }

    public void cancel(LocalDateTime time) {
        status = MonitoringSessionStatus.CANCELLED;
        endedAt = time;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public MonitoringSessionStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
