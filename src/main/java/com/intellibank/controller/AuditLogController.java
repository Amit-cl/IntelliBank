package com.intellibank.controller;

import com.intellibank.dto.AuditLogResponse;
import com.intellibank.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    private String getAuthenticatedUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        throw new AccessDeniedException("Unauthenticated user");
    }

    @GetMapping("/my-logs")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = getAuthenticatedUsername(authentication);
        Page<AuditLogResponse> logs = auditService.getMyLogs(username, pageable);
        return ResponseEntity.ok(logs);
    }
}
