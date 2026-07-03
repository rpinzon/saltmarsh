package com.saltmarsh.repository;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.enums.BerthStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BerthRepository extends JpaRepository<Berth, Long> {
    Optional<Berth> findByCodeIgnoreCase(String code);
    List<Berth> findAllByOrderByPierAscCodeAsc();
    List<Berth> findByStatusOrderByPierAscCodeAsc(BerthStatus status);

    /** Pessimistic lock so concurrent bookings serialize on the berth row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Berth b where b.id = :id")
    Optional<Berth> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select b from Berth b
            where b.status = com.saltmarsh.domain.enums.BerthStatus.AVAILABLE
              and b.maxLengthFeet >= :lengthFeet
              and b.maxDraftFeet >= :draftFeet
              and b.id not in (
                  select r.berth.id from Reservation r
                  where r.status in (
                      com.saltmarsh.domain.enums.ReservationStatus.PENDING,
                      com.saltmarsh.domain.enums.ReservationStatus.CONFIRMED,
                      com.saltmarsh.domain.enums.ReservationStatus.CHECKED_IN
                  )
                  and r.startDate < :endDate
                  and r.endDate > :startDate
              )
              and b.id not in (
                  select w.offeredBerth.id from WaitlistEntry w
                  where w.status = com.saltmarsh.domain.enums.WaitlistStatus.OFFERED
                    and w.offeredBerth is not null
                    and (w.offeredUntil is null or w.offeredUntil > :now)
                    and w.preferredStart < :endDate
                    and w.preferredEnd > :startDate
              )
            order by b.pier asc, b.code asc
            """)
    List<Berth> findAvailableFor(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("lengthFeet") BigDecimal lengthFeet,
            @Param("draftFeet") BigDecimal draftFeet,
            @Param("now") Instant now);
}
