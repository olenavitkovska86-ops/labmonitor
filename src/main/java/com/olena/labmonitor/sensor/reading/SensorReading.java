package com.olena.labmonitor.sensor.reading;

import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.sensor.Sensor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal value;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "safe_min", precision = 12, scale = 3)
    private BigDecimal safeMin;

    @Column(name = "safe_max", precision = 12, scale = 3)
    private BigDecimal safeMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensorReadingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SensorReading() {
    }

    public SensorReading(Sensor sensor, BigDecimal value, LocalDateTime measuredAt) {
        this.sensor = sensor;
        this.room = sensor.getRoom();
        this.value = value;
        this.measuredAt = measuredAt;
        this.safeMin = sensor.getMinSafeValue();
        this.safeMax = sensor.getMaxSafeValue();
        this.status = isOutsideRange(value, safeMin, safeMax)
                ? SensorReadingStatus.OUTSIDE_RANGE
                : SensorReadingStatus.SAFE;
    }

    public Long getId() {
        return id;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public Room getRoom() {
        return room;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getSafeMin() {
        return safeMin;
    }

    public BigDecimal getSafeMax() {
        return safeMax;
    }

    public SensorReadingStatus getStatus() {
        return status;
    }

    private static boolean isOutsideRange(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return (minimum != null && value.compareTo(minimum) < 0)
                || (maximum != null && value.compareTo(maximum) > 0);
    }
}
