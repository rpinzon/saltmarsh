package com.saltmarsh.service;

import com.saltmarsh.domain.AuditEvent;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.repository.AuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UserAccount actor, String action, String entityType, Long entityId, String details) {
        auditEventRepository.save(AuditEvent.of(actor, action, entityType, entityId, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recent() {
        return auditEventRepository.findRecent(PageRequest.of(0, 50));
    }
}
