package com.olena.labmonitor.device.credential;

import com.olena.labmonitor.device.Device;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_credentials")
public class DeviceCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "credential_hash", nullable = false, length = 255)
    private String credentialHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceCredentialStatus status = DeviceCredentialStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DeviceCredential() {
    }

    public DeviceCredential(Device device, String credentialHash, LocalDateTime issuedAt) {
        this.device = device;
        this.credentialHash = credentialHash;
        this.issuedAt = issuedAt;
    }

    public Long getId() { return id; }
    public Device getDevice() { return device; }
    public String getCredentialHash() { return credentialHash; }
    public DeviceCredentialStatus getStatus() { return status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isActive() { return status == DeviceCredentialStatus.ACTIVE; }
    public void recordUsed(LocalDateTime usedAt) { lastUsedAt = usedAt; }
    public void revoke(LocalDateTime time) {
        if (status == DeviceCredentialStatus.REVOKED) return;
        status = DeviceCredentialStatus.REVOKED;
        revokedAt = time;
    }
}
