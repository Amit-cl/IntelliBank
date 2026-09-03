package com.intellibank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellibank.dto.AccountResponse;
import com.intellibank.dto.CreateAccountRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("Should create account and return 201 Created")
    void testCreateAccount_Success() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
        AccountResponse response = new AccountResponse("1234567890", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE, LocalDateTime.now());

        when(accountService.createAccount(eq("customer_user"), any(CreateAccountRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("Should return 400 Bad Request when request body is invalid")
    void testCreateAccount_InvalidRequest() throws Exception {
        // AccountType is null
        CreateAccountRequest request = new CreateAccountRequest(null, new BigDecimal("-100.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("Should return list of accounts for logged in user")
    void testGetMyAccounts_Success() throws Exception {
        AccountResponse acc1 = new AccountResponse("1111111111", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        AccountResponse acc2 = new AccountResponse("2222222222", AccountType.CURRENT, new BigDecimal("2000.00"), AccountStatus.ACTIVE, LocalDateTime.now());

        when(accountService.getMyAccounts("customer_user")).thenReturn(List.of(acc1, acc2));

        mockMvc.perform(get("/api/accounts/my-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("1111111111"))
                .andExpect(jsonPath("$[1].accountNumber").value("2222222222"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("Should return account details by account number")
    void testGetAccountByNumber_Success() throws Exception {
        AccountResponse response = new AccountResponse("1234567890", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE, LocalDateTime.now());

        when(accountService.getAccountByNumber("customer_user", "1234567890")).thenReturn(response);

        mockMvc.perform(get("/api/accounts/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }
}
