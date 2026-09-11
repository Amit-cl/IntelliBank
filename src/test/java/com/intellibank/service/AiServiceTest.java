package com.intellibank.service;

import com.intellibank.dto.AiChatRequest;
import com.intellibank.dto.AiChatResponse;
import com.intellibank.dto.AiSpendingInsightsResponse;
import com.intellibank.entity.*;
import com.intellibank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private GroqApiClient groqApiClient;

    @InjectMocks
    private AiService aiService;

    private User testUser;
    private Customer testCustomer;
    private BankAccount testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("rahul");

        testCustomer = new Customer();
        testCustomer.setId(10L);
        testCustomer.setUser(testUser);
        testCustomer.setFirstName("Rahul");
        testCustomer.setLastName("Sharma");

        testAccount = new BankAccount();
        testAccount.setId(100L);
        testAccount.setAccountNumber("1234567890");
        testAccount.setCustomer(testCustomer);
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(new BigDecimal("50000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should generate spending insights using RAG context and Groq API")
    void testGenerateSpendingInsights_Success() {
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(testAccount));

        Transaction depositTxn = new Transaction();
        depositTxn.setAmount(new BigDecimal("10000.00"));
        depositTxn.setTransactionType(TransactionType.DEPOSIT);
        depositTxn.setTargetAccount(testAccount);

        Transaction withdrawTxn = new Transaction();
        withdrawTxn.setAmount(new BigDecimal("2000.00"));
        withdrawTxn.setTransactionType(TransactionType.WITHDRAWAL);
        withdrawTxn.setSourceAccount(testAccount);
        withdrawTxn.setDescription("Swiggy food delivery");

        when(transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(depositTxn, withdrawTxn)));

        when(auditLogRepository.findByUsernameOrderByCreatedAtDesc(anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(groqApiClient.generateCompletion(anyString(), anyString()))
                .thenReturn("1) You spent ₹2,000 this month. 2) Great job saving ₹8,000 net. 3) Account is secure.");

        AiSpendingInsightsResponse response = aiService.generateSpendingInsights("rahul", "1234567890");

        assertNotNull(response);
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals(new BigDecimal("50000.00"), response.getCurrentBalance());
        assertEquals(new BigDecimal("10000.00"), response.getTotalDeposits());
        assertEquals(new BigDecimal("2000.00"), response.getTotalSpent());
        assertEquals("Dining & Food", response.getTopSpendingCategory());
        assertTrue(response.getAiSummaryAndAdvice().contains("You spent ₹2,000"));
    }

    @Test
    @DisplayName("Should answer chat prompt using authenticated banking context")
    void testChatWithAssistant_Success() {
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByCustomerId(10L)).thenReturn(List.of(testAccount));
        when(transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(auditLogRepository.findByUsernameOrderByCreatedAtDesc(anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(groqApiClient.getModel()).thenReturn("llama-3.3-70b-versatile");
        when(groqApiClient.generateCompletion(anyString(), anyString()))
                .thenReturn("Your balance is ₹50,000.");

        AiChatRequest request = new AiChatRequest("How much balance do I have?", "1234567890");
        AiChatResponse response = aiService.chatWithAssistant("rahul", request);

        assertNotNull(response);
        assertEquals("Your balance is ₹50,000.", response.getReply());
        assertEquals("llama-3.3-70b-versatile", response.getModelUsed());
        assertNotNull(response.getTimestamp());
    }
}
