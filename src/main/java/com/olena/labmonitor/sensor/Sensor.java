package com.olena.labmonitor.sensor;

import com.olena.labmonitor.device.Device;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors", uniqueConstraints =
        @UniqueConstraint(name = "uq_sensors_device_channel", columnNames = {"device_id", "channel_key"}))
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "channel_key", length = 100)
    private String channelKey;

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

    public Device getDevice() { return device; }

    public String getChannelKey() { return channelKey; }

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

    public void assignDeviceChannel(Device device, String channelKey) {
        this.device = device;
        this.channelKey = channelKey;
    }

    public void clearDeviceChannel() {
        this.device = null;
        this.channelKey = null;
    }

    public void recordReading(LocalDateTime measuredAt) {
        this.status = SensorStatus.ONLINE;
        if (lastSeenAt == null || measuredAt.isAfter(lastSeenAt)) {
            this.lastSeenAt = measuredAt;
        }
    }

    public void markOffline() {
        if (status == SensorStatus.ONLINE || status == SensorStatus.OFFLINE) {
            status = SensorStatus.OFFLINE;
        }
    }

    public void deactivate() {
        this.active = false;
        this.status = SensorStatus.OFFLINE;
    }

    public void activate() {
        this.active = true;
        this.status = SensorStatus.OFFLINE;
    }
}
