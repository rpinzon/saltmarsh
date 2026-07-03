package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.WaitlistStatus;
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
import java.time.LocalDate;

@Entity
@Table(name = "waitlist_entry")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vessel_id", nullable = false)
    private Vessel vessel;

    @Column(name = "preferred_start", nullable = false)
    private LocalDate preferredStart;

    @Column(name = "preferred_end", nullable = false)
    private LocalDate preferredEnd;

    @Column(name = "min_length_feet", precision = 6, scale = 2)
    private BigDecimal minLengthFeet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WaitlistStatus status;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_berth_id")
    private Berth offeredBerth;

    @Column(name = "offered_until")
    private Instant offeredUntil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserAccount createdBy;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vessel getVessel() { return vessel; }
    public void setVessel(Vessel vessel) { this.vessel = vessel; }
    public LocalDate getPreferredStart() { return preferredStart; }
    public void setPreferredStart(LocalDate preferredStart) { this.preferredStart = preferredStart; }
    public LocalDate getPreferredEnd() { return preferredEnd; }
    public void setPreferredEnd(LocalDate preferredEnd) { this.preferredEnd = preferredEnd; }
    public BigDecimal getMinLengthFeet() { return minLengthFeet; }
    public void setMinLengthFeet(BigDecimal minLengthFeet) { this.minLengthFeet = minLengthFeet; }
    public WaitlistStatus getStatus() { return status; }
    public void setStatus(WaitlistStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Berth getOfferedBerth() { return offeredBerth; }
    public void setOfferedBerth(Berth offeredBerth) { this.offeredBerth = offeredBerth; }
    public Instant getOfferedUntil() { return offeredUntil; }
    public void setOfferedUntil(Instant offeredUntil) { this.offeredUntil = offeredUntil; }
    public UserAccount getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserAccount createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
