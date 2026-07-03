package com.saltmarsh.web;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.VesselType;
import com.saltmarsh.dto.ReservationRequest;
import com.saltmarsh.exception.ConflictException;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyIntegrationTest {

    @Autowired ReservationService reservationService;
    @Autowired UserAccountRepository users;
    @Autowired VesselRepository vessels;
    @Autowired BerthRepository berths;
    @Autowired ReservationRepository reservations;
    @Autowired PasswordEncoder passwordEncoder;

    UserAccount boaterA;
    UserAccount boaterB;
    Vessel vesselA;
    Vessel vesselB;
    Berth berth;

    @BeforeEach
    void setUp() {
        reservations.deleteAll();
        vessels.deleteAll();
        berths.deleteAll();
        users.deleteAll();

        boaterA = saveUser("conc-a@example.com", "Boater A");
        boaterB = saveUser("conc-b@example.com", "Boater B");
        vesselA = saveVessel(boaterA, "Alpha Boat", "CONC-A");
        vesselB = saveVessel(boaterB, "Bravo Boat", "CONC-B");

        berth = new Berth();
        berth.setCode("C-77");
        berth.setPier("Conc");
        berth.setMaxLengthFeet(new BigDecimal("50"));
        berth.setMaxDraftFeet(new BigDecimal("10"));
        berth.setBerthType(BerthType.TRANSIENT);
        berth.setDailyRate(new BigDecimal("80.00"));
        berth.setStatus(BerthStatus.AVAILABLE);
        berth = berths.save(berth);
    }

    @Test
    void concurrentBookingsOnlyOneSucceeds() throws Exception {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        Callable<Void> bookA = () -> {
            try {
                reservationService.create(
                        new ReservationRequest(vesselA.getId(), berth.getId(), start, end, "A"),
                        boaterA);
                successes.incrementAndGet();
            } catch (ConflictException ex) {
                conflicts.incrementAndGet();
            }
            return null;
        };
        Callable<Void> bookB = () -> {
            try {
                reservationService.create(
                        new ReservationRequest(vesselB.getId(), berth.getId(), start, end, "B"),
                        boaterB);
                successes.incrementAndGet();
            } catch (ConflictException ex) {
                conflicts.incrementAndGet();
            }
            return null;
        };

        List<Future<Void>> futures = pool.invokeAll(List.of(bookA, bookB));
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(1, successes.get(), "exactly one booking should succeed");
        assertEquals(1, conflicts.get(), "exactly one booking should conflict");
        assertEquals(1, reservations.count());
        assertTrue(reservations.findAll().getFirst().getStatus().occupiesBerth());
    }

    private UserAccount saveUser(String email, String name) {
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(Role.BOATER);
        u.setPasswordHash(passwordEncoder.encode("password"));
        u.setEnabled(true);
        return users.save(u);
    }

    private Vessel saveVessel(UserAccount owner, String name, String reg) {
        Vessel v = new Vessel();
        v.setOwner(owner);
        v.setName(name);
        v.setRegistrationNumber(reg);
        v.setLengthFeet(new BigDecimal("28"));
        v.setBeamFeet(new BigDecimal("9"));
        v.setDraftFeet(new BigDecimal("3"));
        v.setVesselType(VesselType.POWER);
        v.setActive(true);
        return vessels.save(v);
    }
}
