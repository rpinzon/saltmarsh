package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "berth")
public class Berth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 32)
    private String pier;

    @Column(name = "max_length_feet", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxLengthFeet;

    @Column(name = "max_draft_feet", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxDraftFeet;

    @Enumerated(EnumType.STRING)
    @Column(name = "berth_type", nullable = false, length = 32)
    private BerthType berthType;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BerthStatus status;

    @Column(length = 500)
    private String notes;

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

    public boolean isBookable() {
        return status == BerthStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPier() { return pier; }
    public void setPier(String pier) { this.pier = pier; }
    public BigDecimal getMaxLengthFeet() { return maxLengthFeet; }
    public void setMaxLengthFeet(BigDecimal maxLengthFeet) { this.maxLengthFeet = maxLengthFeet; }
    public BigDecimal getMaxDraftFeet() { return maxDraftFeet; }
    public void setMaxDraftFeet(BigDecimal maxDraftFeet) { this.maxDraftFeet = maxDraftFeet; }
    public BerthType getBerthType() { return berthType; }
    public void setBerthType(BerthType berthType) { this.berthType = berthType; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public BerthStatus getStatus() { return status; }
    public void setStatus(BerthStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
