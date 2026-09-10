package com.intellibank.controller;

import com.intellibank.dto.AuditLogResponse;
import com.intellibank.entity.AuditAction;
import com.intellibank.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuditLogController.class, AdminAuditLogController.class})
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser(username = "rahul", roles = {"CUSTOMER"})
    @DisplayName("Should return customer's own audit logs")
    void testGetMyAuditLogs_Success() throws Exception {
        AuditLogResponse log = new AuditLogResponse(1L, "rahul", AuditAction.LOGIN, "User logged in", LocalDateTime.now());
        Page<AuditLogResponse> page = new PageImpl<>(List.of(log));

        when(auditService.getMyLogs(eq("rahul"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit-logs/my-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("rahul"))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.content[0].details").value("User logged in"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Should return all system audit logs for admin")
    void testGetAdminAuditLogs_Success() throws Exception {
        AuditLogResponse log1 = new AuditLogResponse(1L, "rahul", AuditAction.DEPOSIT, "Deposit ₹1000", LocalDateTime.now());
        AuditLogResponse log2 = new AuditLogResponse(2L, "admin_user", AuditAction.ACCOUNT_STATUS_CHANGE, "Blocked account", LocalDateTime.now());
        Page<AuditLogResponse> page = new PageImpl<>(List.of(log1, log2));

        when(auditService.getAllLogs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].action").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[1].action").value("ACCOUNT_STATUS_CHANGE"));
    }
}
