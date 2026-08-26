package com.olena.labmonitor.membership;

import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private MembershipScopeType scopeType = MembershipScopeType.ORGANIZATION;

    @ManyToMany
    @JoinTable(
            name = "membership_lab_access",
            joinColumns = @JoinColumn(name = "membership_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_id")
    )
    private Set<Lab> accessibleLabs = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "membership_room_access",
            joinColumns = @JoinColumn(name = "membership_id"),
            inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private Set<Room> accessibleRooms = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Membership() {}

    public Membership(Organization organization, User user, String role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getUser() { return user; }
    public String getRole() { return role; }
    public MembershipScopeType getScopeType() { return scopeType; }
    public Set<Lab> getAccessibleLabs() { return Set.copyOf(accessibleLabs); }
    public Set<Room> getAccessibleRooms() { return Set.copyOf(accessibleRooms); }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateAccess(String role, MembershipScopeType scopeType, Set<Lab> labs, Set<Room> rooms) {
        this.role = role;
        this.scopeType = scopeType;
        this.accessibleLabs.clear();
        this.accessibleRooms.clear();
        if (scopeType == MembershipScopeType.SPECIFIC) {
            this.accessibleLabs.addAll(labs);
            this.accessibleRooms.addAll(rooms);
        }
    }

}
