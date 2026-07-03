package com.saltmarsh.service;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.Invoice;
import com.saltmarsh.domain.InvoiceSequence;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.enums.InvoiceStatus;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.repository.InvoiceRepository;
import com.saltmarsh.repository.InvoiceSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceSequenceRepository invoiceSequenceRepository;
    @Mock AuditService auditService;

    InvoiceService service;
    Clock clock;
    UserAccount customer;
    Reservation reservation;
    InvoiceSequence sequence;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
        service = new InvoiceService(invoiceRepository, invoiceSequenceRepository, auditService, clock);

        customer = new UserAccount();
        customer.setId(1L);
        customer.setRole(Role.BOATER);
        customer.setEmail("alex@example.com");
        customer.setFullName("Alex");

        Vessel vessel = new Vessel();
        vessel.setId(10L);
        vessel.setOwner(customer);
        vessel.setName("Windward");

        Berth berth = new Berth();
        berth.setId(20L);
        berth.setCode("A-01");
        berth.setDailyRate(new BigDecimal("100.00"));

        reservation = new Reservation();
        reservation.setId(50L);
        reservation.setVessel(vessel);
        reservation.setBerth(berth);
        reservation.setStartDate(LocalDate.of(2026, 6, 20));
        reservation.setEndDate(LocalDate.of(2026, 6, 23));
        reservation.setNightlyRate(new BigDecimal("100.00"));
        reservation.setTotalAmount(new BigDecimal("300.00"));
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        sequence = new InvoiceSequence();
        sequence.setId(1);
        sequence.setNextValue(1000L);
    }

    @Test
    void allocatesSequentialNumbersFromDbCounter() {
        when(invoiceRepository.findActiveByReservationId(50L)).thenReturn(Optional.empty());
        when(invoiceSequenceRepository.lockSingleton()).thenReturn(Optional.of(sequence));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Invoice first = service.createFromReservation(reservation, customer);
        assertEquals("INV-20260615-1000", first.getInvoiceNumber());
        assertEquals(InvoiceStatus.ISSUED, first.getStatus());
        assertEquals(new BigDecimal("324.00"), first.getTotalAmount()); // 300 + 8% tax

        // Simulate "restart": new service instance, sequence continues from DB state
        InvoiceService afterRestart = new InvoiceService(
                invoiceRepository, invoiceSequenceRepository, auditService, clock);
        when(invoiceRepository.findActiveByReservationId(99L)).thenReturn(Optional.empty());

        Reservation other = new Reservation();
        other.setId(99L);
        other.setVessel(reservation.getVessel());
        other.setBerth(reservation.getBerth());
        other.setStartDate(LocalDate.of(2026, 6, 24));
        other.setEndDate(LocalDate.of(2026, 6, 25));
        other.setNightlyRate(new BigDecimal("100.00"));
        other.setTotalAmount(new BigDecimal("100.00"));
        other.setStatus(ReservationStatus.CHECKED_OUT);

        Invoice second = afterRestart.createFromReservation(other, customer);
        assertEquals("INV-20260615-1001", second.getInvoiceNumber());
        assertNotEquals(first.getInvoiceNumber(), second.getInvoiceNumber());
        assertTrue(sequence.getNextValue() > 1001 || sequence.getNextValue() == 1002);
    }

    @Test
    void idempotentWhenActiveInvoiceExists() {
        Invoice existing = new Invoice();
        existing.setId(7L);
        existing.setInvoiceNumber("INV-EXISTING");
        existing.setStatus(InvoiceStatus.ISSUED);
        when(invoiceRepository.findActiveByReservationId(50L)).thenReturn(Optional.of(existing));

        Invoice result = service.createFromReservation(reservation, customer);
        assertEquals(7L, result.getId());
        assertEquals("INV-EXISTING", result.getInvoiceNumber());
    }
}
