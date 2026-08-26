package com.olena.labmonitor.membership;

import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Membership() {}

    public Membership(Organization organization, User user, String role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getUser() { return user; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
