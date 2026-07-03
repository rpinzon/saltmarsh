package com.saltmarsh.repository;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.enums.BerthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BerthRepository extends JpaRepository<Berth, Long> {
    Optional<Berth> findByCodeIgnoreCase(String code);
    List<Berth> findAllByOrderByPierAscCodeAsc();
    List<Berth> findByStatusOrderByPierAscCodeAsc(BerthStatus status);

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
            order by b.pier asc, b.code asc
            """)
    List<Berth> findAvailableFor(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("lengthFeet") BigDecimal lengthFeet,
            @Param("draftFeet") BigDecimal draftFeet);
}
