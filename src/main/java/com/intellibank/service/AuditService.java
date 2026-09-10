package com.intellibank.service;

import com.intellibank.dto.AuditLogResponse;
import com.intellibank.entity.AuditAction;
import com.intellibank.entity.AuditLog;
import com.intellibank.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog log(String username, AuditAction action, String details) {
        AuditLog auditLog = new AuditLog(username, action, details);
        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getMyLogs(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                .map(AuditLogResponse::fromEntity);
    }
}
