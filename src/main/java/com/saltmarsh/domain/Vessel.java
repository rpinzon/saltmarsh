package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.VesselType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vessel")
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "registration_number", nullable = false, unique = true, length = 64)
    private String registrationNumber;

    @Column(name = "length_feet", nullable = false, precision = 6, scale = 2)
    private BigDecimal lengthFeet;

    @Column(name = "beam_feet", nullable = false, precision = 6, scale = 2)
    private BigDecimal beamFeet;

    @Column(name = "draft_feet", nullable = false, precision = 6, scale = 2)
    private BigDecimal draftFeet;

    @Enumerated(EnumType.STRING)
    @Column(name = "vessel_type", nullable = false, length = 32)
    private VesselType vesselType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean fitsIn(Berth berth) {
        return lengthFeet.compareTo(berth.getMaxLengthFeet()) <= 0
                && draftFeet.compareTo(berth.getMaxDraftFeet()) <= 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getOwner() { return owner; }
    public void setOwner(UserAccount owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public BigDecimal getLengthFeet() { return lengthFeet; }
    public void setLengthFeet(BigDecimal lengthFeet) { this.lengthFeet = lengthFeet; }
    public BigDecimal getBeamFeet() { return beamFeet; }
    public void setBeamFeet(BigDecimal beamFeet) { this.beamFeet = beamFeet; }
    public BigDecimal getDraftFeet() { return draftFeet; }
    public void setDraftFeet(BigDecimal draftFeet) { this.draftFeet = draftFeet; }
    public VesselType getVesselType() { return vesselType; }
    public void setVesselType(VesselType vesselType) { this.vesselType = vesselType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
