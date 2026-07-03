package com.saltmarsh.web;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.VesselType;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.security.SaltmarshUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReservationFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository users;
    @Autowired VesselRepository vessels;
    @Autowired BerthRepository berths;
    @Autowired ReservationRepository reservations;
    @Autowired PasswordEncoder passwordEncoder;

    UserAccount boater;
    Vessel vessel;
    Berth berth;

    @BeforeEach
    void setUp() {
        // Unique codes/emails so this test coexists with other integration suites
        // sharing the in-memory H2 (some tests commit outside @Transactional).
        String suffix = String.valueOf(System.nanoTime());

        boater = new UserAccount();
        boater.setEmail("flow-boater-" + suffix + "@example.com");
        boater.setFullName("Flow Boater");
        boater.setRole(Role.BOATER);
        boater.setPasswordHash(passwordEncoder.encode("password"));
        boater.setEnabled(true);
        boater = users.save(boater);

        vessel = new Vessel();
        vessel.setOwner(boater);
        vessel.setName("Flow Boat");
        vessel.setRegistrationNumber("FLOW-" + suffix);
        vessel.setLengthFeet(new BigDecimal("28"));
        vessel.setBeamFeet(new BigDecimal("9"));
        vessel.setDraftFeet(new BigDecimal("3"));
        vessel.setVesselType(VesselType.POWER);
        vessel.setActive(true);
        vessel = vessels.save(vessel);

        berth = new Berth();
        berth.setCode("T" + suffix.substring(Math.max(0, suffix.length() - 8)));
        berth.setPier("Test");
        berth.setMaxLengthFeet(new BigDecimal("40"));
        berth.setMaxDraftFeet(new BigDecimal("8"));
        berth.setBerthType(BerthType.TRANSIENT);
        berth.setDailyRate(new BigDecimal("50.00"));
        berth.setStatus(BerthStatus.AVAILABLE);
        berth = berths.save(berth);
    }

    @Test
    void boaterCanCreatePendingReservation() throws Exception {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.plusDays(2);
        long before = reservations.count();

        mockMvc.perform(post("/reservations")
                        .with(user(new SaltmarshUserDetails(boater)))
                        .with(csrf())
                        .param("vesselId", vessel.getId().toString())
                        .param("berthId", berth.getId().toString())
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("notes", "integration test"))
                .andExpect(status().is3xxRedirection());

        assertEquals(before + 1, reservations.count());
        var created = reservations.findAll().stream()
                .filter(r -> r.getVessel().getId().equals(vessel.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("PENDING", created.getStatus().name());
        assertEquals(new BigDecimal("100.00"), created.getTotalAmount());
    }
}
