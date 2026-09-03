package com.intellibank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellibank.dto.AccountResponse;
import com.intellibank.dto.UpdateAccountStatusRequest;
import com.intellibank.entity.AccountStatus;
import com.intellibank.entity.AccountType;
import com.intellibank.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("Should update account status successfully")
    void testUpdateAccountStatus_Success() throws Exception {
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.BLOCKED);
        AccountResponse response = new AccountResponse("1234567890", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.BLOCKED, LocalDateTime.now());

        when(accountService.updateAccountStatus(eq("1234567890"), eq(AccountStatus.BLOCKED)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/accounts/1234567890/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should return all accounts for admin")
    void testGetAllAccounts_Success() throws Exception {
        AccountResponse acc1 = new AccountResponse("1111111111", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        AccountResponse acc2 = new AccountResponse("2222222222", AccountType.CURRENT, new BigDecimal("2000.00"), AccountStatus.BLOCKED, LocalDateTime.now());

        when(accountService.getAllAccounts()).thenReturn(List.of(acc1, acc2));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("1111111111"))
                .andExpect(jsonPath("$[1].accountNumber").value("2222222222"));
    }
}
