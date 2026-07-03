package com.saltmarsh.service;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.WaitlistEntry;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.WaitlistStatus;
import com.saltmarsh.dto.WaitlistRequest;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final VesselRepository vesselRepository;
    private final ReservationRepository reservationRepository;
    private final BerthRepository berthRepository;
    private final AuditService auditService;
    private final ReservationService reservationService;
    private final WaitlistOfferExpiryService offerExpiryService;
    private final Clock clock;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository,
                           VesselRepository vesselRepository,
                           ReservationRepository reservationRepository,
                           BerthRepository berthRepository,
                           AuditService auditService,
                           ReservationService reservationService,
                           WaitlistOfferExpiryService offerExpiryService,
                           Clock clock) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.vesselRepository = vesselRepository;
        this.reservationRepository = reservationRepository;
        this.berthRepository = berthRepository;
        this.auditService = auditService;
        this.reservationService = reservationService;
        this.offerExpiryService = offerExpiryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> listFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return waitlistEntryRepository.findByStatusOrderByCreatedAtAsc(WaitlistStatus.WAITING);
        }
        return waitlistEntryRepository.findByOwnerId(actor.getId());
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> listAllActive() {
        List<WaitlistEntry> waiting = waitlistEntryRepository.findByStatusOrderByCreatedAtAsc(WaitlistStatus.WAITING);
        List<WaitlistEntry> offered = waitlistEntryRepository.findByStatusOrderByCreatedAtAsc(WaitlistStatus.OFFERED);
        waiting.addAll(offered);
        return waiting;
    }

    @Transactional
    public WaitlistEntry join(WaitlistRequest request, UserAccount actor) {
        LocalDate today = LocalDate.now(clock);
        if (request.preferredStart().isBefore(today)) {
            throw new BusinessException("INVALID_DATES", "Preferred start cannot be in the past");
        }
        if (!request.preferredEnd().isAfter(request.preferredStart())) {
            throw new BusinessException("INVALID_DATES", "Preferred end must be after start");
        }

        Vessel vessel = vesselRepository.findByIdWithOwner(request.vesselId())
                .orElseThrow(() -> new NotFoundException("Vessel not found"));
        if (!actor.getRole().isStaffOrAbove() && !vessel.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only waitlist your own vessels");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setVessel(vessel);
        entry.setPreferredStart(request.preferredStart());
        entry.setPreferredEnd(request.preferredEnd());
        entry.setMinLengthFeet(request.minLengthFeet());
        entry.setNotes(request.notes());
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setCreatedBy(actor);

        WaitlistEntry saved = waitlistEntryRepository.save(entry);
        auditService.record(actor, "WAITLIST_JOINED", "WaitlistEntry", saved.getId(),
                "Waitlist for " + vessel.getName() + " " + request.preferredStart()
                        + " → " + request.preferredEnd());
        return waitlistEntryRepository.findDetailedById(saved.getId()).orElse(saved);
    }

    @Transactional
    public Reservation acceptOffer(Long id, UserAccount actor) {
        // Commit expiries independently so a failed accept cannot leave OFFERED forever.
        offerExpiryService.returnExpiredOffersToQueue();

        WaitlistEntry entry = waitlistEntryRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Waitlist entry not found"));

        if (!actor.getRole().isStaffOrAbove()
                && !entry.getVessel().getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot accept this offer");
        }
        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BusinessException("NOT_OFFERED",
                    "No active offer on this waitlist entry (it may have expired and returned to the queue)");
        }
        if (entry.getOfferedUntil() != null && !entry.getOfferedUntil().isAfter(Instant.now(clock))) {
            // Defensive: expiry service should have already returned this to WAITING.
            throw new BusinessException("OFFER_EXPIRED", "The berth offer has expired");
        }

        Long berthId = entry.getOfferedBerth() != null ? entry.getOfferedBerth().getId() : null;
        if (berthId == null) {
            throw new ConflictException("Offered berth is no longer available");
        }

        Berth berth = berthRepository.findByIdForUpdate(berthId)
                .orElseThrow(() -> new ConflictException("Offered berth is no longer available"));
        if (!berth.isBookable()) {
            throw new ConflictException("Offered berth is no longer available");
        }
        if (!entry.getVessel().fitsIn(berth)) {
            throw new BusinessException("VESSEL_TOO_LARGE", "Vessel no longer fits the offered berth");
        }

        // Exclude this waitlist hold when checking occupancy; it is being converted.
        reservationService.assertBerthFree(
                berth.getId(), entry.getPreferredStart(), entry.getPreferredEnd(), entry.getId());

        Reservation reservation = new Reservation();
        reservation.setVessel(entry.getVessel());
        reservation.setBerth(berth);
        reservation.setStartDate(entry.getPreferredStart());
        reservation.setEndDate(entry.getPreferredEnd());
        reservation.setNightlyRate(berth.getDailyRate());
        reservation.setTotalAmount(Reservation.calculateTotal(
                berth.getDailyRate(), entry.getPreferredStart(), entry.getPreferredEnd()));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedBy(actor);
        reservation.setNotes("Accepted from waitlist #" + entry.getId());

        Reservation saved = reservationRepository.save(reservation);
        entry.setStatus(WaitlistStatus.ACCEPTED);
        entry.setOfferedBerth(null);
        entry.setOfferedUntil(null);

        auditService.record(actor, "WAITLIST_ACCEPTED", "WaitlistEntry", entry.getId(),
                "Created reservation #" + saved.getId() + " on berth " + berth.getCode());
        return reservationRepository.findDetailedById(saved.getId()).orElse(saved);
    }

    @Transactional
    public void cancel(Long id, UserAccount actor) {
        WaitlistEntry entry = waitlistEntryRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Waitlist entry not found"));
        if (!actor.getRole().isStaffOrAbove()
                && !entry.getVessel().getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot cancel this waitlist entry");
        }
        if (entry.getStatus() == WaitlistStatus.ACCEPTED || entry.getStatus() == WaitlistStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "Cannot cancel entry in status " + entry.getStatus());
        }
        entry.setStatus(WaitlistStatus.CANCELLED);
        entry.setOfferedBerth(null);
        entry.setOfferedUntil(null);
        auditService.record(actor, "WAITLIST_CANCELLED", "WaitlistEntry", entry.getId(), "Waitlist cancelled");
    }
}
