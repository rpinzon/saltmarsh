package com.saltmarsh.config;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.WaitlistEntry;
import com.saltmarsh.domain.WorkOrder;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.VesselType;
import com.saltmarsh.domain.enums.WaitlistStatus;
import com.saltmarsh.domain.enums.WorkOrderCategory;
import com.saltmarsh.domain.enums.WorkOrderPriority;
import com.saltmarsh.domain.enums.WorkOrderStatus;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.repository.WaitlistEntryRepository;
import com.saltmarsh.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!test")
    CommandLineRunner seedData(UserAccountRepository users,
                               VesselRepository vessels,
                               BerthRepository berths,
                               ReservationRepository reservations,
                               WaitlistEntryRepository waitlist,
                               WorkOrderRepository workOrders,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            log.info("Seeding demo data...");

            UserAccount admin = user(users, passwordEncoder, "admin@saltmarsh.harbor", "Harbor Admin", Role.ADMIN, "password");
            UserAccount harbor = user(users, passwordEncoder, "harbor@saltmarsh.harbor", "Maya Chen", Role.HARBORMASTER, "password");
            UserAccount staff = user(users, passwordEncoder, "staff@saltmarsh.harbor", "Jordan Blake", Role.STAFF, "password");
            UserAccount alex = user(users, passwordEncoder, "alex@example.com", "Alex Rivera", Role.BOATER, "password");
            UserAccount sam = user(users, passwordEncoder, "sam@example.com", "Sam Okonkwo", Role.BOATER, "password");
            UserAccount riley = user(users, passwordEncoder, "riley@example.com", "Riley Nguyen", Role.BOATER, "password");

            Vessel windward = vessel(vessels, alex, "Windward", "US-4421-A", "32.0", "10.5", "4.5", VesselType.SAIL);
            Vessel seaforge = vessel(vessels, alex, "Seaforge", "US-8810-B", "28.0", "9.0", "3.2", VesselType.POWER);
            Vessel northstar = vessel(vessels, sam, "Northstar", "US-2200-C", "40.0", "13.0", "5.5", VesselType.SAIL);
            Vessel tidepool = vessel(vessels, riley, "Tidepool", "US-1199-D", "24.0", "8.5", "2.8", VesselType.CATAMARAN);

            Berth a1 = berth(berths, "A-01", "Alpha", "35", "6", BerthType.TRANSIENT, "85.00");
            Berth a2 = berth(berths, "A-02", "Alpha", "30", "5", BerthType.TRANSIENT, "70.00");
            Berth a3 = berth(berths, "A-03", "Alpha", "45", "7", BerthType.SEASONAL, "95.00");
            Berth b1 = berth(berths, "B-01", "Bravo", "50", "8", BerthType.LIVEABOARD, "120.00");
            Berth b2 = berth(berths, "B-02", "Bravo", "28", "4", BerthType.TRANSIENT, "65.00");
            Berth b3 = berth(berths, "B-03", "Bravo", "36", "6", BerthType.TRANSIENT, "80.00");
            Berth c1 = berth(berths, "C-01", "Charlie", "42", "7", BerthType.SEASONAL, "100.00");
            Berth c2 = berth(berths, "C-02", "Charlie", "26", "4", BerthType.TRANSIENT, "60.00");
            c2.setStatus(BerthStatus.MAINTENANCE);
            berths.save(c2);

            LocalDate today = LocalDate.now();

            Reservation r1 = reservation(reservations, windward, a1, today.minusDays(1), today.plusDays(4),
                    ReservationStatus.CHECKED_IN, harbor);
            r1.setCheckedInAt(java.time.Instant.now().minusSeconds(3600 * 20));
            reservations.save(r1);

            reservation(reservations, northstar, a3, today.plusDays(2), today.plusDays(9),
                    ReservationStatus.CONFIRMED, sam);

            reservation(reservations, tidepool, b2, today.plusDays(1), today.plusDays(3),
                    ReservationStatus.PENDING, riley);

            reservation(reservations, seaforge, b3, today.plusDays(10), today.plusDays(14),
                    ReservationStatus.CONFIRMED, alex);

            WaitlistEntry wl = new WaitlistEntry();
            wl.setVessel(northstar);
            wl.setPreferredStart(today.plusDays(1));
            wl.setPreferredEnd(today.plusDays(5));
            wl.setStatus(WaitlistStatus.WAITING);
            wl.setNotes("Prefer Alpha pier if possible");
            wl.setCreatedBy(sam);
            waitlist.save(wl);

            WorkOrder wo1 = new WorkOrder();
            wo1.setTitle("Shore power pedestal intermittent");
            wo1.setDescription("Pedestal at A-01 drops power under load. Guest reported overnight.");
            wo1.setPriority(WorkOrderPriority.HIGH);
            wo1.setCategory(WorkOrderCategory.ELECTRICAL);
            wo1.setStatus(WorkOrderStatus.ASSIGNED);
            wo1.setBerth(a1);
            wo1.setVessel(windward);
            wo1.setReportedBy(harbor);
            wo1.setAssignedTo(staff);
            workOrders.save(wo1);

            WorkOrder wo2 = new WorkOrder();
            wo2.setTitle("Engine oil change request");
            wo2.setDescription("Scheduled maintenance for Tidepool before weekend trip.");
            wo2.setPriority(WorkOrderPriority.MEDIUM);
            wo2.setCategory(WorkOrderCategory.ENGINE);
            wo2.setStatus(WorkOrderStatus.OPEN);
            wo2.setVessel(tidepool);
            wo2.setReportedBy(riley);
            workOrders.save(wo2);

            WorkOrder wo3 = new WorkOrder();
            wo3.setTitle("Dock cleat loose on Bravo pier");
            wo3.setDescription("Mid-pier cleat near B-02 is spinning. Safety hazard.");
            wo3.setPriority(WorkOrderPriority.URGENT);
            wo3.setCategory(WorkOrderCategory.DOCK);
            wo3.setStatus(WorkOrderStatus.IN_PROGRESS);
            wo3.setBerth(b2);
            wo3.setReportedBy(staff);
            wo3.setAssignedTo(staff);
            wo3.setStartedAt(java.time.Instant.now().minusSeconds(7200));
            workOrders.save(wo3);

            log.info("Demo data ready. Log in with any seeded user / password 'password'");
            log.info("  admin@saltmarsh.harbor, harbor@saltmarsh.harbor, staff@saltmarsh.harbor");
            log.info("  alex@example.com, sam@example.com, riley@example.com");
        };
    }

    private static UserAccount user(UserAccountRepository repo, PasswordEncoder encoder,
                                    String email, String name, Role role, String password) {
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(password));
        u.setEnabled(true);
        u.setPhone("555-0100");
        return repo.save(u);
    }

    private static Vessel vessel(VesselRepository repo, UserAccount owner, String name, String reg,
                                 String length, String beam, String draft, VesselType type) {
        Vessel v = new Vessel();
        v.setOwner(owner);
        v.setName(name);
        v.setRegistrationNumber(reg);
        v.setLengthFeet(new BigDecimal(length));
        v.setBeamFeet(new BigDecimal(beam));
        v.setDraftFeet(new BigDecimal(draft));
        v.setVesselType(type);
        v.setActive(true);
        return repo.save(v);
    }

    private static Berth berth(BerthRepository repo, String code, String pier, String maxLen,
                               String maxDraft, BerthType type, String rate) {
        Berth b = new Berth();
        b.setCode(code);
        b.setPier(pier);
        b.setMaxLengthFeet(new BigDecimal(maxLen));
        b.setMaxDraftFeet(new BigDecimal(maxDraft));
        b.setBerthType(type);
        b.setDailyRate(new BigDecimal(rate));
        b.setStatus(BerthStatus.AVAILABLE);
        return repo.save(b);
    }

    private static Reservation reservation(ReservationRepository repo, Vessel vessel, Berth berth,
                                           LocalDate start, LocalDate end, ReservationStatus status,
                                           UserAccount createdBy) {
        Reservation r = new Reservation();
        r.setVessel(vessel);
        r.setBerth(berth);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setStatus(status);
        r.setNightlyRate(berth.getDailyRate());
        r.setTotalAmount(Reservation.calculateTotal(berth.getDailyRate(), start, end));
        r.setCreatedBy(createdBy);
        return repo.save(r);
    }
}
