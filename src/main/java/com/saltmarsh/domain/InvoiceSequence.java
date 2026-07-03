package com.saltmarsh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single-row table used to allocate invoice numbers under a pessimistic lock.
 */
@Entity
@Table(name = "invoice_sequence")
public class InvoiceSequence {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    public InvoiceSequence() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Long getNextValue() {
        return nextValue;
    }

    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }

    /** Atomically consume the current value and advance the counter. */
    public long allocateNext() {
        long allocated = nextValue;
        nextValue = nextValue + 1;
        return allocated;
    }
}
