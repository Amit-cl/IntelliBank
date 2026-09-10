package com.intellibank.dto;

import com.intellibank.entity.AuditAction;
import com.intellibank.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String username;
    private AuditAction action;
    private String details;
    private LocalDateTime timestamp;

    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUsername(),
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
