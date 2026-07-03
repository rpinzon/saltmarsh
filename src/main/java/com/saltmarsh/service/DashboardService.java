package com.saltmarsh.service;

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

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
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
}
