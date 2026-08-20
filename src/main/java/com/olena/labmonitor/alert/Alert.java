package com.olena.labmonitor.alert;

import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 30)
    private AlertResolutionOutcome resolutionOutcome;

    @Column(name = "resolution_comment", length = 1000)
    private String resolutionComment;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reopened_by_user_id")
    private User reopenedByUser;

    @Column(name = "violation_started_at")
    private LocalDateTime violationStartedAt;

    @Column(name = "initial_value", precision = 12, scale = 3)
    private BigDecimal initialValue;

    @Column(name = "latest_value", precision = 12, scale = 3)
    private BigDecimal latestValue;

    @Column(name = "most_extreme_value", precision = 12, scale = 3)
    private BigDecimal mostExtremeValue;

    @Column(name = "last_violation_at")
    private LocalDateTime lastViolationAt;

    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

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

    public void resolve(User user, AlertResolutionOutcome outcome, String comment) {
        status = AlertStatus.RESOLVED;
        resolvedAt = LocalDateTime.now();
        resolvedByUser = user;
        resolutionOutcome = outcome;
        resolutionComment = comment == null || comment.isBlank() ? null : comment.trim();
    }

    public void reopen(User user) {
        status = AlertStatus.ACTIVE;
        acknowledgedAt = null;
        acknowledgedByUser = null;
        resolvedAt = null;
        resolvedByUser = null;
        resolutionOutcome = null;
        resolutionComment = null;
        reopenedAt = LocalDateTime.now();
        reopenedByUser = user;
    }

    public void startThresholdViolation(BigDecimal value, LocalDateTime measuredAt) {
        violationStartedAt = measuredAt;
        initialValue = value;
        latestValue = value;
        mostExtremeValue = value;
        lastViolationAt = measuredAt;
    }

    public void updateThresholdViolation(
            BigDecimal value,
            BigDecimal extremeValue,
            AlertSeverity newSeverity,
            LocalDateTime measuredAt
    ) {
        if (lastViolationAt == null || !measuredAt.isBefore(lastViolationAt)) {
            latestValue = value;
            lastViolationAt = measuredAt;
            recoveredAt = null;
        }
        mostExtremeValue = extremeValue;
        if (newSeverity.ordinal() > severity.ordinal()) {
            severity = newSeverity;
        }
    }

    public void markRecovered(LocalDateTime measuredAt) {
        if (lastViolationAt == null || !measuredAt.isBefore(lastViolationAt)) {
            recoveredAt = measuredAt;
        }
    }

    public void resolveAutomatically(LocalDateTime measuredAt) {
        status = AlertStatus.RESOLVED;
        resolvedAt = measuredAt;
        resolvedByUser = null;
        resolutionOutcome = AlertResolutionOutcome.AUTO_RECOVERED;
        resolutionComment = "Sensor value returned to the safe range";
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
    public AlertResolutionOutcome getResolutionOutcome() { return resolutionOutcome; }
    public String getResolutionComment() { return resolutionComment; }
    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public User getReopenedByUser() { return reopenedByUser; }
    public LocalDateTime getViolationStartedAt() { return violationStartedAt; }
    public BigDecimal getInitialValue() { return initialValue; }
    public BigDecimal getLatestValue() { return latestValue; }
    public BigDecimal getMostExtremeValue() { return mostExtremeValue; }
    public LocalDateTime getLastViolationAt() { return lastViolationAt; }
    public LocalDateTime getRecoveredAt() { return recoveredAt; }
}
