package com.saltmarsh.service;

import com.saltmarsh.config.SaltmarshProperties;
import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.WaitlistEntry;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.WaitlistStatus;
import com.saltmarsh.dto.ReservationRequest;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ConflictException;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationService {

    private static final List<ReservationStatus> OCCUPYING = List.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );

    private final ReservationRepository reservationRepository;
    private final VesselRepository vesselRepository;
    private final BerthRepository berthRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AuditService auditService;
    private final InvoiceService invoiceService;
    private final SaltmarshProperties properties;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              VesselRepository vesselRepository,
                              BerthRepository berthRepository,
                              WaitlistEntryRepository waitlistEntryRepository,
                              AuditService auditService,
                              InvoiceService invoiceService,
                              SaltmarshProperties properties,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.vesselRepository = vesselRepository;
        this.berthRepository = berthRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.auditService = auditService;
        this.invoiceService = invoiceService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Reservation> listFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return reservationRepository.findAllDetailed();
        }
        return reservationRepository.findByOwnerId(actor.getId());
    }

    @Transactional(readOnly = true)
    public Reservation getVisible(Long id, UserAccount actor) {
        Reservation reservation = reservationRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        assertCanView(reservation, actor);
        return reservation;
    }

    @Transactional(readOnly = true)
    public List<Reservation> currentlyDocked() {
        return reservationRepository.findCurrentlyDocked();
    }

    @Transactional(readOnly = true)
    public List<Reservation> currentlyDockedFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return reservationRepository.findCurrentlyDocked();
        }
        return reservationRepository.findCurrentlyDockedByOwner(actor.getId());
    }

    @Transactional
    public Reservation create(ReservationRequest request, UserAccount actor) {
        LocalDate today = LocalDate.now(clock);
        if (request.startDate().isBefore(today)) {
            throw new BusinessException("INVALID_DATES", "Start date cannot be in the past");
        }
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessException("INVALID_DATES", "End date must be after start date");
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 90) {
            throw new BusinessException("STAY_TOO_LONG", "Maximum stay is 90 nights");
        }

        Vessel vessel = vesselRepository.findByIdWithOwner(request.vesselId())
                .orElseThrow(() -> new NotFoundException("Vessel not found"));
        if (!vessel.isActive()) {
            throw new BusinessException("VESSEL_INACTIVE", "Vessel is not active");
        }
        assertOwnsVesselOrStaff(vessel, actor);

        // Lock berth row first so concurrent create/accept serialize on inventory.
        Berth berth = berthRepository.findByIdForUpdate(request.berthId())
                .orElseThrow(() -> new NotFoundException("Berth not found"));
        if (!berth.isBookable()) {
            throw new ConflictException("Berth is not available for booking (status: " + berth.getStatus() + ")");
        }
        if (!vessel.fitsIn(berth)) {
            throw new BusinessException("VESSEL_TOO_LARGE",
                    "Vessel dimensions exceed berth capacity (max length "
                            + berth.getMaxLengthFeet() + " ft, max draft "
                            + berth.getMaxDraftFeet() + " ft)");
        }

        assertBerthFree(berth.getId(), request.startDate(), request.endDate(), null);

        Reservation reservation = new Reservation();
        reservation.setVessel(vessel);
        reservation.setBerth(berth);
        reservation.setStartDate(request.startDate());
        reservation.setEndDate(request.endDate());
        reservation.setNightlyRate(berth.getDailyRate());
        reservation.setTotalAmount(Reservation.calculateTotal(berth.getDailyRate(),
                request.startDate(), request.endDate()));
        reservation.setNotes(request.notes());
        reservation.setCreatedBy(actor);

        // Staff/harbormaster can confirm immediately; boaters start as PENDING
        if (actor.getRole().isStaffOrAbove()) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            reservation.setStatus(ReservationStatus.PENDING);
        }

        Reservation saved = reservationRepository.save(reservation);
        auditService.record(actor, "RESERVATION_CREATED", "Reservation", saved.getId(),
                "Berth " + berth.getCode() + " for " + vessel.getName()
                        + " " + request.startDate() + " → " + request.endDate()
                        + " [" + saved.getStatus() + "]");
        return reservationRepository.findDetailedById(saved.getId()).orElse(saved);
    }

    @Transactional
    public Reservation confirm(Long id, UserAccount actor) {
        requireStaff(actor);
        Reservation reservation = getVisible(id, actor);
        transition(reservation, ReservationStatus.CONFIRMED, actor, "RESERVATION_CONFIRMED");
        return reservation;
    }

    @Transactional
    public Reservation checkIn(Long id, UserAccount actor) {
        requireStaff(actor);
        Reservation reservation = getVisible(id, actor);
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(reservation.getStartDate().minusDays(1))) {
            throw new BusinessException("TOO_EARLY", "Cannot check in more than one day before start date");
        }
        if (today.isAfter(reservation.getEndDate())) {
            throw new BusinessException("TOO_LATE", "Stay window has already ended");
        }
        reservation.setCheckedInAt(Instant.now(clock));
        transition(reservation, ReservationStatus.CHECKED_IN, actor, "RESERVATION_CHECKED_IN");
        return reservation;
    }

    @Transactional
    public Reservation checkOut(Long id, UserAccount actor) {
        requireStaff(actor);
        Reservation reservation = getVisible(id, actor);
        Berth berth = reservation.getBerth();
        LocalDate start = reservation.getStartDate();
        LocalDate end = reservation.getEndDate();
        reservation.setCheckedOutAt(Instant.now(clock));
        transition(reservation, ReservationStatus.CHECKED_OUT, actor, "RESERVATION_CHECKED_OUT");
        // Full reserved-stay billing (non-refundable for unused tail nights after early departure).
        invoiceService.createFromReservation(reservation, actor);
        promoteWaitlist(berth, start, end, actor);
        return reservation;
    }

    @Transactional
    public Reservation markNoShow(Long id, UserAccount actor) {
        requireStaff(actor);
        Reservation reservation = getVisible(id, actor);
        transition(reservation, ReservationStatus.NO_SHOW, actor, "RESERVATION_NO_SHOW");
        promoteWaitlist(reservation.getBerth(), reservation.getStartDate(), reservation.getEndDate(), actor);
        return reservation;
    }

    @Transactional
    public Reservation cancel(Long id, UserAccount actor) {
        Reservation reservation = getVisible(id, actor);
        boolean owner = reservation.getVessel().getOwner().getId().equals(actor.getId());
        if (!owner && !actor.getRole().isStaffOrAbove()) {
            throw new ForbiddenException("You cannot cancel this reservation");
        }
        if (!reservation.getStatus().canTransitionTo(ReservationStatus.CANCELLED)) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Cannot cancel a reservation in status " + reservation.getStatus());
        }

        BigDecimal fee = calculateLateCancelFee(reservation);
        reservation.setLateCancelFee(fee);
        reservation.setCancelledAt(Instant.now(clock));
        reservation.setStatus(ReservationStatus.CANCELLED);

        auditService.record(actor, "RESERVATION_CANCELLED", "Reservation", reservation.getId(),
                "Cancelled with late fee " + fee);

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            invoiceService.createCancellationFeeInvoice(reservation, actor);
        }

        promoteWaitlist(reservation.getBerth(), reservation.getStartDate(), reservation.getEndDate(), actor);
        return reservation;
    }

    /**
     * Ensures no overlapping occupying reservation or active waitlist offer holds the berth.
     * Caller must hold a pessimistic lock on the berth row when creating bookings.
     */
    public void assertBerthFree(Long berthId, LocalDate start, LocalDate end, Long excludeWaitlistId) {
        if (reservationRepository.hasOverlapping(berthId, start, end, OCCUPYING, null)) {
            throw new ConflictException("Berth is already reserved for overlapping dates");
        }
        if (waitlistEntryRepository.hasActiveOfferOccupying(
                berthId, start, end, Instant.now(clock), excludeWaitlistId)) {
            throw new ConflictException("Berth is held by an active waitlist offer for overlapping dates");
        }
    }

    private BigDecimal calculateLateCancelFee(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }
        Instant startInstant = reservation.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        long hoursUntil = ChronoUnit.HOURS.between(Instant.now(clock), startInstant);
        int freeHours = properties.cancellation().freeCancelHours();
        if (hoursUntil >= freeHours) {
            return BigDecimal.ZERO;
        }
        int percent = properties.cancellation().lateFeePercent();
        return reservation.getTotalAmount()
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void promoteWaitlist(Berth berth, LocalDate start, LocalDate end, UserAccount actor) {
        returnExpiredOffersToQueue();
        List<WaitlistEntry> waiting = waitlistEntryRepository.findWaitingOverlapping(start, end);
        for (WaitlistEntry entry : waiting) {
            Vessel vessel = entry.getVessel();
            if (!vessel.fitsIn(berth)) {
                continue;
            }
            if (entry.getMinLengthFeet() != null
                    && berth.getMaxLengthFeet().compareTo(entry.getMinLengthFeet()) < 0) {
                continue;
            }
            if (reservationRepository.hasOverlapping(berth.getId(),
                    entry.getPreferredStart(), entry.getPreferredEnd(), OCCUPYING, null)) {
                continue;
            }
            if (waitlistEntryRepository.hasActiveOfferOccupying(
                    berth.getId(), entry.getPreferredStart(), entry.getPreferredEnd(),
                    Instant.now(clock), entry.getId())) {
                continue;
            }
            entry.setStatus(WaitlistStatus.OFFERED);
            entry.setOfferedBerth(berth);
            entry.setOfferedUntil(Instant.now(clock).plus(24, ChronoUnit.HOURS));
            auditService.record(actor, "WAITLIST_OFFERED", "WaitlistEntry", entry.getId(),
                    "Offered berth " + berth.getCode() + " until " + entry.getOfferedUntil());
            break; // FIFO: only first matching entry
        }
    }

    /**
     * Expired offers return to WAITING so the entry re-enters FIFO rather than dying forever.
     */
    @Transactional
    public void returnExpiredOffersToQueue() {
        List<WaitlistEntry> expired = waitlistEntryRepository.findExpiredOffers(Instant.now(clock));
        for (WaitlistEntry entry : expired) {
            entry.setStatus(WaitlistStatus.WAITING);
            entry.setOfferedBerth(null);
            entry.setOfferedUntil(null);
        }
    }

    private void transition(Reservation reservation, ReservationStatus target, UserAccount actor, String action) {
        if (!reservation.getStatus().canTransitionTo(target)) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Cannot transition from " + reservation.getStatus() + " to " + target);
        }
        reservation.setStatus(target);
        auditService.record(actor, action, "Reservation", reservation.getId(),
                "Status → " + target);
    }

    private void assertCanView(Reservation reservation, UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return;
        }
        if (!reservation.getVessel().getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot view this reservation");
        }
    }

    private void assertOwnsVesselOrStaff(Vessel vessel, UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return;
        }
        if (!vessel.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only reserve berths for your own vessels");
        }
    }

    private void requireStaff(UserAccount actor) {
        if (!actor.getRole().isStaffOrAbove()) {
            throw new ForbiddenException("Staff access required");
        }
    }
}
