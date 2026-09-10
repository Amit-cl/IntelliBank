package com.intellibank.service;

import com.intellibank.dto.AuditLogResponse;
import com.intellibank.entity.AuditAction;
import com.intellibank.entity.AuditLog;
import com.intellibank.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = new AuditLog("rahul", AuditAction.LOGIN, "User logged in successfully");
        sampleLog.setId(1L);
        sampleLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully create and save audit log entry")
    void testLog_Success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        AuditLog saved = auditService.log("rahul", AuditAction.LOGIN, "User logged in successfully");

        assertNotNull(saved);
        assertEquals("rahul", saved.getUsername());
        assertEquals(AuditAction.LOGIN, saved.getAction());
        assertEquals("User logged in successfully", saved.getDetails());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should return paginated audit logs for a specific customer")
    void testGetMyLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> logPage = new PageImpl<>(List.of(sampleLog));
        when(auditLogRepository.findByUsernameOrderByCreatedAtDesc("rahul", pageable)).thenReturn(logPage);

        Page<AuditLogResponse> result = auditService.getMyLogs("rahul", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("rahul", result.getContent().get(0).getUsername());
        assertEquals(AuditAction.LOGIN, result.getContent().get(0).getAction());
    }

    @Test
    @DisplayName("Should return all system audit logs for admin")
    void testGetAllLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> logPage = new PageImpl<>(List.of(sampleLog));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(logPage);

        Page<AuditLogResponse> result = auditService.getAllLogs(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("rahul", result.getContent().get(0).getUsername());
    }

    @Test
    @DisplayName("Should return audit logs filtered by action")
    void testGetLogsByAction() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> logPage = new PageImpl<>(List.of(sampleLog));
        when(auditLogRepository.findByActionOrderByCreatedAtDesc(AuditAction.LOGIN, pageable)).thenReturn(logPage);

        Page<AuditLogResponse> result = auditService.getLogsByAction(AuditAction.LOGIN, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(AuditAction.LOGIN, result.getContent().get(0).getAction());
    }
}
