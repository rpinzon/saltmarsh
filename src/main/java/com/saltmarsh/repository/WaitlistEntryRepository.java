package com.saltmarsh.repository;

import com.saltmarsh.domain.WaitlistEntry;
import com.saltmarsh.domain.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    @Query("""
            select w from WaitlistEntry w
            join fetch w.vessel v
            join fetch v.owner
            left join fetch w.offeredBerth
            where w.id = :id
            """)
    Optional<WaitlistEntry> findDetailedById(@Param("id") Long id);

    @Query("""
            select w from WaitlistEntry w
            join fetch w.vessel v
            join fetch v.owner
            left join fetch w.offeredBerth
            where w.status = :status
            order by w.createdAt asc
            """)
    List<WaitlistEntry> findByStatusOrderByCreatedAtAsc(@Param("status") WaitlistStatus status);

    @Query("""
            select w from WaitlistEntry w
            join fetch w.vessel v
            join fetch v.owner
            left join fetch w.offeredBerth
            where v.owner.id = :ownerId
            order by w.createdAt desc
            """)
    List<WaitlistEntry> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            select w from WaitlistEntry w
            join fetch w.vessel v
            join fetch v.owner
            where w.status = com.saltmarsh.domain.enums.WaitlistStatus.WAITING
              and w.preferredStart < :endDate
              and w.preferredEnd > :startDate
            order by w.createdAt asc
            """)
    List<WaitlistEntry> findWaitingOverlapping(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select w from WaitlistEntry w
            join fetch w.vessel
            left join fetch w.offeredBerth
            where w.status = com.saltmarsh.domain.enums.WaitlistStatus.OFFERED
              and w.offeredUntil < :now
            """)
    List<WaitlistEntry> findExpiredOffers(@Param("now") Instant now);

    /**
     * Active waitlist offers occupy the berth for the offered date range so other
     * boaters cannot book through the hold window.
     */
    @Query("""
            select case when count(w) > 0 then true else false end
            from WaitlistEntry w
            where w.offeredBerth.id = :berthId
              and w.status = com.saltmarsh.domain.enums.WaitlistStatus.OFFERED
              and (w.offeredUntil is null or w.offeredUntil > :now)
              and w.preferredStart < :endDate
              and w.preferredEnd > :startDate
              and (:excludeId is null or w.id <> :excludeId)
            """)
    boolean hasActiveOfferOccupying(
            @Param("berthId") Long berthId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") Instant now,
            @Param("excludeId") Long excludeId);
}
