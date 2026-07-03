package com.saltmarsh.service;

import com.saltmarsh.domain.WaitlistEntry;
import com.saltmarsh.domain.enums.WaitlistStatus;
import com.saltmarsh.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Commits waitlist offer expirations in an independent transaction so a subsequent
 * business failure (e.g. accepting an already-expired offer) cannot roll the expiry back.
 */
@Service
public class WaitlistOfferExpiryService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final Clock clock;

    public WaitlistOfferExpiryService(WaitlistEntryRepository waitlistEntryRepository, Clock clock) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void returnExpiredOffersToQueue() {
        List<WaitlistEntry> expired = waitlistEntryRepository.findExpiredOffers(Instant.now(clock));
        for (WaitlistEntry entry : expired) {
            entry.setStatus(WaitlistStatus.WAITING);
            entry.setOfferedBerth(null);
            entry.setOfferedUntil(null);
        }
    }
}
