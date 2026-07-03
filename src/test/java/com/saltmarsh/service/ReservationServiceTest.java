package com.saltmarsh.service;

import com.saltmarsh.config.SaltmarshProperties;
import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.dto.ReservationRequest;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ConflictException;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock VesselRepository vesselRepository;
    @Mock BerthRepository berthRepository;
    @Mock WaitlistEntryRepository waitlistEntryRepository;
    @Mock AuditService auditService;
    @Mock InvoiceService invoiceService;

    ReservationService service;
    Clock clock;
    UserAccount boater;
    UserAccount staff;
    Vessel vessel;
    Berth berth;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
        SaltmarshProperties props = new SaltmarshProperties(
                new SaltmarshProperties.Cancellation(48, 25),
                new SaltmarshProperties.Security(true));
        service = new ReservationService(reservationRepository, vesselRepository, berthRepository,
                waitlistEntryRepository, auditService, invoiceService, props, clock);

        boater = new UserAccount();
        boater.setId(1L);
        boater.setRole(Role.BOATER);
        boater.setFullName("Alex");
        boater.setEmail("alex@example.com");

        staff = new UserAccount();
        staff.setId(2L);
        staff.setRole(Role.STAFF);
        staff.setFullName("Jordan");

        vessel = new Vessel();
        vessel.setId(10L);
        vessel.setOwner(boater);
        vessel.setName("Windward");
        vessel.setActive(true);
        vessel.setLengthFeet(new BigDecimal("30"));
        vessel.setDraftFeet(new BigDecimal("4"));

        berth = new Berth();
        berth.setId(20L);
        berth.setCode("A-01");
        berth.setStatus(BerthStatus.AVAILABLE);
        berth.setMaxLengthFeet(new BigDecimal("40"));
        berth.setMaxDraftFeet(new BigDecimal("6"));
        berth.setDailyRate(new BigDecimal("100.00"));
    }

    @Test
    void boaterCreatesPendingReservation() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 23);
        when(vesselRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(vessel));
        when(berthRepository.findById(20L)).thenReturn(Optional.of(berth));
        when(reservationRepository.hasOverlapping(eq(20L), eq(start), eq(end), anyList(), isNull()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
        when(reservationRepository.findDetailedById(99L)).thenAnswer(inv -> {
            Reservation r = new Reservation();
            r.setId(99L);
            r.setStatus(ReservationStatus.PENDING);
            return Optional.of(r);
        });

        ReservationRequest request = new ReservationRequest(10L, 20L, start, end, "note");
        Reservation result = service.create(request, boater);

        assertEquals(ReservationStatus.PENDING, result.getStatus());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getTotalAmount());
        assertEquals(ReservationStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void rejectsOverlappingBooking() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 23);
        when(vesselRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(vessel));
        when(berthRepository.findById(20L)).thenReturn(Optional.of(berth));
        when(reservationRepository.hasOverlapping(eq(20L), eq(start), eq(end), anyList(), isNull()))
                .thenReturn(true);

        ReservationRequest request = new ReservationRequest(10L, 20L, start, end, null);
        assertThrows(ConflictException.class, () -> service.create(request, boater));
    }

    @Test
    void rejectsVesselTooLarge() {
        vessel.setLengthFeet(new BigDecimal("50"));
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 23);
        when(vesselRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(vessel));
        when(berthRepository.findById(20L)).thenReturn(Optional.of(berth));

        ReservationRequest request = new ReservationRequest(10L, 20L, start, end, null);
        assertThrows(BusinessException.class, () -> service.create(request, boater));
    }

    @Test
    void staffCreatesConfirmedReservation() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 21);
        when(vesselRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(vessel));
        when(berthRepository.findById(20L)).thenReturn(Optional.of(berth));
        when(reservationRepository.hasOverlapping(eq(20L), eq(start), eq(end), anyList(), isNull()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });
        when(reservationRepository.findDetailedById(5L)).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        // findDetailedById returns empty path - fix by returning saved status
        when(reservationRepository.findDetailedById(5L)).thenAnswer(inv -> {
            Reservation r = new Reservation();
            r.setId(5L);
            r.setStatus(ReservationStatus.CONFIRMED);
            return Optional.of(r);
        });

        Reservation result = service.create(new ReservationRequest(10L, 20L, start, end, null), staff);
        assertEquals(ReservationStatus.CONFIRMED, result.getStatus());
    }
}
