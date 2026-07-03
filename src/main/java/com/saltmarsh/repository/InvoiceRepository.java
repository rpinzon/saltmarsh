package com.saltmarsh.repository;

import com.saltmarsh.domain.Invoice;
import com.saltmarsh.domain.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            select i from Invoice i
            join fetch i.customer
            left join fetch i.reservation
            left join fetch i.workOrder
            left join fetch i.lineItems
            where i.id = :id
            """)
    Optional<Invoice> findDetailedById(@Param("id") Long id);

    @Query("""
            select distinct i from Invoice i
            join fetch i.customer
            left join fetch i.reservation
            left join fetch i.workOrder
            order by i.createdAt desc
            """)
    List<Invoice> findAllDetailed();

    @Query("""
            select distinct i from Invoice i
            join fetch i.customer
            left join fetch i.reservation
            left join fetch i.workOrder
            where i.customer.id = :customerId
            order by i.createdAt desc
            """)
    List<Invoice> findByCustomerId(@Param("customerId") Long customerId);

    boolean existsByReservationIdAndStatusNot(Long reservationId, InvoiceStatus status);
    boolean existsByWorkOrderIdAndStatusNot(Long workOrderId, InvoiceStatus status);

    long countByStatus(InvoiceStatus status);
}
