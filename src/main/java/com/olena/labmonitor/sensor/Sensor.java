package com.olena.labmonitor.sensor;

import com.olena.labmonitor.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SensorType type;

    @Column(length = 30)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensorStatus status = SensorStatus.OFFLINE;

    @Column(name = "min_safe_value", precision = 12, scale = 3)
    private BigDecimal minSafeValue;

    @Column(name = "max_safe_value", precision = 12, scale = 3)
    private BigDecimal maxSafeValue;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Sensor() {
    }

    public Sensor(Room room, String name, SensorType type, String unit) {
        this.room = room;
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getName() {
        return name;
    }

    public SensorType getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public BigDecimal getMinSafeValue() {
        return minSafeValue;
    }

    public BigDecimal getMaxSafeValue() {
        return maxSafeValue;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    public void updateSafeRange(BigDecimal minSafeValue, BigDecimal maxSafeValue) {
        this.minSafeValue = minSafeValue;
        this.maxSafeValue = maxSafeValue;
    }

    public void recordReading(LocalDateTime measuredAt) {
        this.status = SensorStatus.ONLINE;
        if (lastSeenAt == null || measuredAt.isAfter(lastSeenAt)) {
            this.lastSeenAt = measuredAt;
        }
    }

    public void deactivate() {
        this.active = false;
    }
}
