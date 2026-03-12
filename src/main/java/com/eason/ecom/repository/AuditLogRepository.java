package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId, Pageable pageable);

    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    List<AuditLog> findByEntityIdOrderByCreatedAtDesc(String entityId, Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
