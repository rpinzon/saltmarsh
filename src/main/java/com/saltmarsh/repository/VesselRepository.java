package com.saltmarsh.repository;

import com.saltmarsh.domain.Vessel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VesselRepository extends JpaRepository<Vessel, Long> {

    @Query("""
            select v from Vessel v
            join fetch v.owner
            where v.owner.id = :ownerId and v.active = true
            order by v.name asc
            """)
    List<Vessel> findByOwnerIdAndActiveTrueOrderByNameAsc(@Param("ownerId") Long ownerId);

    @Query("""
            select v from Vessel v
            join fetch v.owner
            where v.active = true
            order by v.name asc
            """)
    List<Vessel> findByActiveTrueOrderByNameAsc();

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    Optional<Vessel> findByIdAndActiveTrue(Long id);

    @Query("""
            select v from Vessel v
            join fetch v.owner
            where v.id = :id
            """)
    Optional<Vessel> findByIdWithOwner(@Param("id") Long id);
}
