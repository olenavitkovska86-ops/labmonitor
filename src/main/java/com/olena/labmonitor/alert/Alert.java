package com.olena.labmonitor.alert;

import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by_user_id")
    private User acknowledgedByUser;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedByUser;

    protected Alert() {
    }

    public Alert(Room room, Sensor sensor, AlertType type, AlertSeverity severity, String title, String message) {
        this.room = room;
        this.sensor = sensor;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
    }

    public void acknowledge(User user) {
        status = AlertStatus.ACKNOWLEDGED;
        acknowledgedAt = LocalDateTime.now();
        acknowledgedByUser = user;
    }

    public void resolve(User user) {
        status = AlertStatus.RESOLVED;
        resolvedAt = LocalDateTime.now();
        resolvedByUser = user;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Sensor getSensor() { return sensor; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public User getAcknowledgedByUser() { return acknowledgedByUser; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public User getResolvedByUser() { return resolvedByUser; }
}
