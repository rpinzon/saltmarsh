package com.saltmarsh.repository;

import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select r from Reservation r
            join fetch r.vessel v
            join fetch v.owner
            join fetch r.berth
            join fetch r.createdBy
            where r.id = :id
            """)
    Optional<Reservation> findDetailedById(@Param("id") Long id);

    @Query("""
            select r from Reservation r
            join fetch r.vessel v
            join fetch v.owner
            join fetch r.berth
            where v.owner.id = :ownerId
            order by r.startDate desc
            """)
    List<Reservation> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            select r from Reservation r
            join fetch r.vessel
            join fetch r.berth
            order by r.startDate desc
            """)
    List<Reservation> findAllDetailed();

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.berth.id = :berthId
              and r.status in :statuses
              and r.startDate < :endDate
              and r.endDate > :startDate
              and (:excludeId is null or r.id <> :excludeId)
            """)
    boolean hasOverlapping(
            @Param("berthId") Long berthId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("excludeId") Long excludeId);

    @Query("""
            select r from Reservation r
            join fetch r.vessel
            join fetch r.berth
            where r.status = com.saltmarsh.domain.enums.ReservationStatus.CHECKED_IN
            order by r.berth.code asc
            """)
    List<Reservation> findCurrentlyDocked();

    @Query("""
            select r from Reservation r
            join fetch r.vessel v
            join fetch r.berth
            where r.status = com.saltmarsh.domain.enums.ReservationStatus.CHECKED_IN
              and v.owner.id = :ownerId
            order by r.berth.code asc
            """)
    List<Reservation> findCurrentlyDockedByOwner(@Param("ownerId") Long ownerId);

    long countByStatus(ReservationStatus status);

    long countByVesselOwnerIdAndStatus(Long ownerId, ReservationStatus status);
}
