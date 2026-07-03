package com.saltmarsh.service;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.InvoiceStatus;
import com.saltmarsh.domain.enums.ReservationStatus;
import com.saltmarsh.domain.enums.WorkOrderStatus;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.InvoiceRepository;
import com.saltmarsh.repository.ReservationRepository;
import com.saltmarsh.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final BerthRepository berthRepository;
    private final ReservationRepository reservationRepository;
    private final WorkOrderRepository workOrderRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardService(BerthRepository berthRepository,
                            ReservationRepository reservationRepository,
                            WorkOrderRepository workOrderRepository,
                            InvoiceRepository invoiceRepository) {
        this.berthRepository = berthRepository;
        this.reservationRepository = reservationRepository;
        this.workOrderRepository = workOrderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Staff see marina-wide ops metrics; boaters only see counts for their own activity.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> statsFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return marinaStats();
        }
        return boaterStats(actor.getId());
    }

    private Map<String, Object> marinaStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("scope", "marina");
        stats.put("totalBerths", berthRepository.count());
        stats.put("availableBerths", berthRepository.findByStatusOrderByPierAscCodeAsc(
                com.saltmarsh.domain.enums.BerthStatus.AVAILABLE).size());
        stats.put("dockedNow", reservationRepository.countByStatus(ReservationStatus.CHECKED_IN));
        stats.put("pendingReservations", reservationRepository.countByStatus(ReservationStatus.PENDING));
        stats.put("confirmedReservations", reservationRepository.countByStatus(ReservationStatus.CONFIRMED));
        stats.put("openWorkOrders", workOrderRepository.countByStatusIn(List.of(
                WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED,
                WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.BLOCKED)));
        stats.put("issuedInvoices", invoiceRepository.countByStatus(InvoiceStatus.ISSUED));
        stats.put("paidInvoices", invoiceRepository.countByStatus(InvoiceStatus.PAID));
        return stats;
    }

    private Map<String, Object> boaterStats(Long ownerId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("scope", "personal");
        stats.put("totalBerths", null);
        stats.put("availableBerths", null);
        stats.put("dockedNow", reservationRepository.countByVesselOwnerIdAndStatus(
                ownerId, ReservationStatus.CHECKED_IN));
        stats.put("pendingReservations", reservationRepository.countByVesselOwnerIdAndStatus(
                ownerId, ReservationStatus.PENDING));
        stats.put("confirmedReservations", reservationRepository.countByVesselOwnerIdAndStatus(
                ownerId, ReservationStatus.CONFIRMED));
        // Work orders and invoices for boaters use customer/owner scoping at list level;
        // expose unpaid invoice count for the boater only.
        stats.put("openWorkOrders", null);
        stats.put("issuedInvoices", invoiceRepository.countByCustomerIdAndStatus(
                ownerId, InvoiceStatus.ISSUED));
        stats.put("paidInvoices", invoiceRepository.countByCustomerIdAndStatus(
                ownerId, InvoiceStatus.PAID));
        return stats;
    }
}
