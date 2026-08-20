package com.olena.labmonitor.alert.history;

import com.olena.labmonitor.alert.Alert;
import com.olena.labmonitor.alert.AlertResolutionOutcome;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 30)
    private AlertResolutionOutcome resolutionOutcome;

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AlertHistory() {
    }

    public AlertHistory(
            Alert alert,
            User performedByUser,
            AlertHistoryAction action,
            AlertResolutionOutcome resolutionOutcome,
            String comment
    ) {
        this.alert = alert;
        this.performedByUser = performedByUser;
        this.action = action;
        this.resolutionOutcome = resolutionOutcome;
        this.comment = comment == null || comment.isBlank() ? null : comment.trim();
    }

    public static AlertHistory autoRecovered(Alert alert) {
        return new AlertHistory(
                alert,
                null,
                AlertHistoryAction.AUTO_RECOVERED,
                AlertResolutionOutcome.AUTO_RECOVERED,
                "Sensor value returned to the safe range"
        );
    }

    public AlertHistoryAction getAction() { return action; }
    public AlertResolutionOutcome getResolutionOutcome() { return resolutionOutcome; }
    public String getComment() { return comment; }
    public User getPerformedByUser() { return performedByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
