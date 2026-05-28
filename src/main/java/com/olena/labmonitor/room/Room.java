package com.olena.labmonitor.room;

import com.olena.labmonitor.lab.Lab;
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
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomType type;

    private Integer floor;

    @Column(name = "area_m2", precision = 6, scale = 2)
    private BigDecimal area;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Room() {
    }

    public Room(Lab lab, String name, RoomType type, Integer floor, BigDecimal area) {
        this.lab = lab;
        this.name = name;
        this.type = type;
        this.floor = floor;
        this.area = area;
    }

    public Long getId() {
        return id;
    }

    public Lab getLab() {
        return lab;
    }

    public String getName() {
        return name;
    }

    public RoomType getType() {
        return type;
    }

    public Integer getFloor() {
        return floor;
    }

    public BigDecimal getArea() {
        return area;
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

    public void update(String name, RoomType type, Integer floor, BigDecimal area) {
        this.name = name;
        this.type = type;
        this.floor = floor;
        this.area = area;
    }

    public void deactivate() {
        this.active = false;
    }
}
