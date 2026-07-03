package com.saltmarsh.repository;

import com.saltmarsh.domain.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select a from AuditEvent a
            left join fetch a.actor
            order by a.createdAt desc
            """)
    List<AuditEvent> findRecent(Pageable pageable);
}
