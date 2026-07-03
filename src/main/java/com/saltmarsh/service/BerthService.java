package com.saltmarsh.service;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import com.saltmarsh.dto.BerthRequest;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.BerthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class BerthService {

    private final BerthRepository berthRepository;
    private final AuditService auditService;
    private final Clock clock;

    public BerthService(BerthRepository berthRepository, AuditService auditService, Clock clock) {
        this.berthRepository = berthRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Berth> listAll() {
        return berthRepository.findAllByOrderByPierAscCodeAsc();
    }

    @Transactional(readOnly = true)
    public Berth get(Long id) {
        return berthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Berth not found"));
    }

    @Transactional(readOnly = true)
    public List<Berth> findAvailable(LocalDate start, LocalDate end, BigDecimal lengthFeet, BigDecimal draftFeet) {
        if (!end.isAfter(start)) {
            throw new BusinessException("INVALID_DATES", "End date must be after start date");
        }
        return berthRepository.findAvailableFor(start, end, lengthFeet, draftFeet, Instant.now(clock));
    }

    @Transactional
    public Berth create(BerthRequest request, UserAccount actor) {
        if (berthRepository.findByCodeIgnoreCase(request.code().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Berth code already exists");
        }
        Berth berth = new Berth();
        apply(berth, request);
        Berth saved = berthRepository.save(berth);
        auditService.record(actor, "BERTH_CREATED", "Berth", saved.getId(), "Created berth " + saved.getCode());
        return saved;
    }

    @Transactional
    public Berth update(Long id, BerthRequest request, UserAccount actor) {
        Berth berth = get(id);
        berthRepository.findByCodeIgnoreCase(request.code().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("DUPLICATE_CODE", "Berth code already exists");
            }
        });
        apply(berth, request);
        auditService.record(actor, "BERTH_UPDATED", "Berth", berth.getId(), "Updated berth " + berth.getCode());
        return berth;
    }

    @Transactional
    public Berth updateStatus(Long id, BerthStatus status, UserAccount actor) {
        Berth berth = get(id);
        berth.setStatus(status);
        auditService.record(actor, "BERTH_STATUS", "Berth", berth.getId(),
                "Status set to " + status + " for " + berth.getCode());
        return berth;
    }

    private void apply(Berth berth, BerthRequest request) {
        berth.setCode(request.code().trim().toUpperCase());
        berth.setPier(request.pier().trim());
        berth.setMaxLengthFeet(request.maxLengthFeet());
        berth.setMaxDraftFeet(request.maxDraftFeet());
        berth.setBerthType(BerthType.valueOf(request.berthType()));
        berth.setDailyRate(request.dailyRate());
        berth.setStatus(BerthStatus.valueOf(request.status()));
        berth.setNotes(request.notes());
    }
}
