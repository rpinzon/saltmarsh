package com.saltmarsh.service;

import com.saltmarsh.domain.Invoice;
import com.saltmarsh.domain.InvoiceLineItem;
import com.saltmarsh.domain.Reservation;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.WorkOrder;
import com.saltmarsh.domain.enums.InvoiceStatus;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InvoiceService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong(1000);

    public InvoiceService(InvoiceRepository invoiceRepository,
                          AuditService auditService,
                          Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Invoice> listFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return invoiceRepository.findAllDetailed();
        }
        return invoiceRepository.findByCustomerId(actor.getId());
    }

    @Transactional(readOnly = true)
    public Invoice getVisible(Long id, UserAccount actor) {
        Invoice invoice = invoiceRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (!actor.getRole().isStaffOrAbove()
                && !invoice.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot view this invoice");
        }
        return invoice;
    }

    @Transactional
    public Invoice createFromReservation(Reservation reservation, UserAccount actor) {
        if (invoiceRepository.existsByReservationIdAndStatusNot(reservation.getId(), InvoiceStatus.VOID)) {
            return invoiceRepository.findAllDetailed().stream()
                    .filter(i -> i.getReservation() != null
                            && i.getReservation().getId().equals(reservation.getId())
                            && i.getStatus() != InvoiceStatus.VOID)
                    .findFirst()
                    .orElseThrow();
        }

        Invoice invoice = baseInvoice(reservation.getVessel().getOwner());
        invoice.setReservation(reservation);

        long nights = reservation.nights();
        invoice.addLineItem(InvoiceLineItem.of(
                "Berth " + reservation.getBerth().getCode() + " — " + nights + " night(s)",
                BigDecimal.valueOf(nights),
                reservation.getNightlyRate()));

        if (reservation.getLateCancelFee() != null
                && reservation.getLateCancelFee().compareTo(BigDecimal.ZERO) > 0) {
            invoice.addLineItem(InvoiceLineItem.of(
                    "Late cancellation fee",
                    BigDecimal.ONE,
                    reservation.getLateCancelFee()));
        }

        finalizeAmounts(invoice);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now(clock));
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(actor, "INVOICE_ISSUED", "Invoice", saved.getId(),
                "From reservation #" + reservation.getId() + " total " + saved.getTotalAmount());
        return saved;
    }

    @Transactional
    public Invoice createCancellationFeeInvoice(Reservation reservation, UserAccount actor) {
        if (reservation.getLateCancelFee() == null
                || reservation.getLateCancelFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("NO_FEE", "No cancellation fee to invoice");
        }
        Invoice invoice = baseInvoice(reservation.getVessel().getOwner());
        invoice.setReservation(reservation);
        invoice.addLineItem(InvoiceLineItem.of(
                "Late cancellation fee — reservation #" + reservation.getId(),
                BigDecimal.ONE,
                reservation.getLateCancelFee()));
        finalizeAmounts(invoice);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now(clock));
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(actor, "INVOICE_ISSUED", "Invoice", saved.getId(),
                "Cancellation fee for reservation #" + reservation.getId());
        return saved;
    }

    @Transactional
    public Invoice createFromWorkOrder(WorkOrder workOrder, UserAccount actor) {
        if (workOrder.getVessel() == null) {
            throw new BusinessException("NO_CUSTOMER", "Work order has no vessel/customer to bill");
        }
        if (invoiceRepository.existsByWorkOrderIdAndStatusNot(workOrder.getId(), InvoiceStatus.VOID)) {
            return invoiceRepository.findAllDetailed().stream()
                    .filter(i -> i.getWorkOrder() != null
                            && i.getWorkOrder().getId().equals(workOrder.getId())
                            && i.getStatus() != InvoiceStatus.VOID)
                    .findFirst()
                    .orElseThrow();
        }

        Invoice invoice = baseInvoice(workOrder.getVessel().getOwner());
        invoice.setWorkOrder(workOrder);

        if (workOrder.getLaborHours() != null && workOrder.getLaborHours().compareTo(BigDecimal.ZERO) > 0) {
            invoice.addLineItem(InvoiceLineItem.of(
                    "Labor — " + workOrder.getTitle(),
                    workOrder.getLaborHours(),
                    workOrder.getLaborRate()));
        }
        if (workOrder.getPartsCost() != null && workOrder.getPartsCost().compareTo(BigDecimal.ZERO) > 0) {
            invoice.addLineItem(InvoiceLineItem.of(
                    "Parts — " + workOrder.getTitle(),
                    BigDecimal.ONE,
                    workOrder.getPartsCost()));
        }

        if (invoice.getLineItems().isEmpty()) {
            invoice.addLineItem(InvoiceLineItem.of(
                    "Service — " + workOrder.getTitle(),
                    BigDecimal.ONE,
                    BigDecimal.ZERO));
        }

        finalizeAmounts(invoice);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now(clock));
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(actor, "INVOICE_ISSUED", "Invoice", saved.getId(),
                "From work order #" + workOrder.getId() + " total " + saved.getTotalAmount());
        return saved;
    }

    @Transactional
    public Invoice markPaid(Long id, UserAccount actor) {
        requireStaff(actor);
        Invoice invoice = getVisible(id, actor);
        if (!invoice.getStatus().canTransitionTo(InvoiceStatus.PAID)) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Cannot mark invoice as paid from status " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now(clock));
        auditService.record(actor, "INVOICE_PAID", "Invoice", invoice.getId(),
                "Paid " + invoice.getTotalAmount());
        return invoice;
    }

    @Transactional
    public Invoice voidInvoice(Long id, UserAccount actor) {
        requireStaff(actor);
        Invoice invoice = getVisible(id, actor);
        if (!invoice.getStatus().canTransitionTo(InvoiceStatus.VOID)) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Cannot void invoice in status " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidedAt(Instant.now(clock));
        auditService.record(actor, "INVOICE_VOIDED", "Invoice", invoice.getId(), "Invoice voided");
        return invoice;
    }

    private Invoice baseInvoice(UserAccount customer) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextNumber());
        invoice.setCustomer(customer);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        return invoice;
    }

    private void finalizeAmounts(Invoice invoice) {
        BigDecimal subtotal = invoice.getLineItems().stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(subtotal.add(tax));
    }

    private String nextNumber() {
        String date = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "INV-" + date + "-" + sequence.incrementAndGet();
    }

    private void requireStaff(UserAccount actor) {
        if (!actor.getRole().isStaffOrAbove()) {
            throw new ForbiddenException("Staff access required");
        }
    }
}
