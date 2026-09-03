package com.intellibank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellibank.dto.DepositRequest;
import com.intellibank.dto.TransactionResponse;
import com.intellibank.dto.TransferRequest;
import com.intellibank.dto.WithdrawRequest;
import com.intellibank.entity.TransactionStatus;
import com.intellibank.entity.TransactionType;
import com.intellibank.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "alice", roles = {"CUSTOMER"})
    @DisplayName("Should successfully deposit funds")
    void testDeposit_Success() throws Exception {
        DepositRequest request = new DepositRequest("1111111111", new BigDecimal("1000.00"), "Cash deposit");
        TransactionResponse response = new TransactionResponse(
                "TXN-12345", TransactionType.DEPOSIT, new BigDecimal("1000.00"),
                TransactionStatus.SUCCESS, null, "1111111111", "Cash deposit", LocalDateTime.now());

        when(transactionService.deposit(eq("alice"), any(DepositRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-12345"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"CUSTOMER"})
    @DisplayName("Should successfully withdraw funds")
    void testWithdraw_Success() throws Exception {
        WithdrawRequest request = new WithdrawRequest("1111111111", new BigDecimal("500.00"), "ATM withdraw");
        TransactionResponse response = new TransactionResponse(
                "TXN-12346", TransactionType.WITHDRAWAL, new BigDecimal("500.00"),
                TransactionStatus.SUCCESS, "1111111111", null, "ATM withdraw", LocalDateTime.now());

        when(transactionService.withdraw(eq("alice"), any(WithdrawRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-12346"))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"CUSTOMER"})
    @DisplayName("Should successfully transfer funds")
    void testTransfer_Success() throws Exception {
        TransferRequest request = new TransferRequest("1111111111", "2222222222", new BigDecimal("750.00"), "Peer transfer");
        TransactionResponse response = new TransactionResponse(
                "TXN-12347", TransactionType.TRANSFER, new BigDecimal("750.00"),
                TransactionStatus.SUCCESS, "1111111111", "2222222222", "Peer transfer", LocalDateTime.now());

        when(transactionService.transfer(eq("alice"), any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-12347"))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceAccountNumber").value("1111111111"))
                .andExpect(jsonPath("$.targetAccountNumber").value("2222222222"));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"CUSTOMER"})
    @DisplayName("Should return paginated transaction history")
    void testGetTransactionHistory_Success() throws Exception {
        TransactionResponse txn = new TransactionResponse(
                "TXN-12348", TransactionType.DEPOSIT, new BigDecimal("500.00"),
                TransactionStatus.SUCCESS, null, "1111111111", "Initial deposit", LocalDateTime.now());

        when(transactionService.getTransactionHistory(eq("alice"), eq("1111111111"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn)));

        mockMvc.perform(get("/api/transactions/history/1111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionReference").value("TXN-12348"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
