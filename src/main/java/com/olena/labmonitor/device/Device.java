package com.olena.labmonitor.device;

import com.olena.labmonitor.organization.Organization;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Device() {
    }

    public Device(Organization organization, String name, DeviceType type) {
        this.organization = organization;
        this.name = name;
        this.type = type;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public DeviceType getType() { return type; }
    public DeviceStatus getStatus() { return status; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void recordSeen(LocalDateTime seenAt) {
        if (lastSeenAt == null || seenAt.isAfter(lastSeenAt)) lastSeenAt = seenAt;
    }

    public void disable() { status = DeviceStatus.DISABLED; }
    public void activate() { status = DeviceStatus.ACTIVE; }
}
