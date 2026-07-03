package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.WorkOrderCategory;
import com.saltmarsh.domain.enums.WorkOrderPriority;
import com.saltmarsh.domain.enums.WorkOrderStatus;
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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_id")
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berth_id")
    private Berth berth;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkOrderCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private UserAccount reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private UserAccount assignedTo;

    @Column(name = "labor_hours", precision = 6, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "parts_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name = "labor_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborRate = new BigDecimal("85.00");

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

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

    public BigDecimal estimatedTotal() {
        BigDecimal labor = BigDecimal.ZERO;
        if (laborHours != null) {
            labor = laborHours.multiply(laborRate).setScale(2, RoundingMode.HALF_UP);
        }
        return labor.add(partsCost == null ? BigDecimal.ZERO : partsCost).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vessel getVessel() { return vessel; }
    public void setVessel(Vessel vessel) { this.vessel = vessel; }
    public Berth getBerth() { return berth; }
    public void setBerth(Berth berth) { this.berth = berth; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public WorkOrderPriority getPriority() { return priority; }
    public void setPriority(WorkOrderPriority priority) { this.priority = priority; }
    public WorkOrderStatus getStatus() { return status; }
    public void setStatus(WorkOrderStatus status) { this.status = status; }
    public WorkOrderCategory getCategory() { return category; }
    public void setCategory(WorkOrderCategory category) { this.category = category; }
    public UserAccount getReportedBy() { return reportedBy; }
    public void setReportedBy(UserAccount reportedBy) { this.reportedBy = reportedBy; }
    public UserAccount getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UserAccount assignedTo) { this.assignedTo = assignedTo; }
    public BigDecimal getLaborHours() { return laborHours; }
    public void setLaborHours(BigDecimal laborHours) { this.laborHours = laborHours; }
    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public BigDecimal getLaborRate() { return laborRate; }
    public void setLaborRate(BigDecimal laborRate) { this.laborRate = laborRate; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
