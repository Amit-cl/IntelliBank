package com.intellibank.service;

import com.intellibank.dto.DepositRequest;
import com.intellibank.dto.TransactionResponse;
import com.intellibank.dto.TransferRequest;
import com.intellibank.dto.WithdrawRequest;
import com.intellibank.entity.*;
import com.intellibank.exception.AccountInactiveException;
import com.intellibank.exception.InsufficientBalanceException;
import com.intellibank.exception.InvalidTransactionException;
import com.intellibank.repository.BankAccountRepository;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.TransactionRepository;
import com.intellibank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Customer testCustomer;
    private BankAccount sourceAccount;
    private BankAccount targetAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");

        testCustomer = new Customer();
        testCustomer.setId(10L);
        testCustomer.setUser(testUser);

        sourceAccount = new BankAccount();
        sourceAccount.setId(100L);
        sourceAccount.setAccountNumber("1111111111");
        sourceAccount.setCustomer(testCustomer);
        sourceAccount.setBalance(new BigDecimal("5000.00"));
        sourceAccount.setStatus(AccountStatus.ACTIVE);
        sourceAccount.setAccountType(AccountType.SAVINGS);

        Customer otherCustomer = new Customer();
        otherCustomer.setId(20L);

        targetAccount = new BankAccount();
        targetAccount.setId(200L);
        targetAccount.setAccountNumber("2222222222");
        targetAccount.setCustomer(otherCustomer);
        targetAccount.setBalance(new BigDecimal("1000.00"));
        targetAccount.setStatus(AccountStatus.ACTIVE);
        targetAccount.setAccountType(AccountType.CURRENT);
    }

    @Test
    @DisplayName("Should successfully deposit money into account")
    void testDeposit_Success() {
        DepositRequest request = new DepositRequest("1111111111", new BigDecimal("1000.00"), "Salary deposit");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(1L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.deposit("alice", request);

        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals("1111111111", response.getTargetAccountNumber());
        assertEquals(new BigDecimal("6000.00"), sourceAccount.getBalance());

        verify(bankAccountRepository).save(sourceAccount);
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when depositing into blocked account")
    void testDeposit_InactiveAccount() {
        sourceAccount.setStatus(AccountStatus.BLOCKED);
        DepositRequest request = new DepositRequest("1111111111", new BigDecimal("1000.00"), null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));

        assertThrows(AccountInactiveException.class, () ->
                transactionService.deposit("alice", request));
    }

    @Test
    @DisplayName("Should successfully withdraw money from account")
    void testWithdraw_Success() {
        WithdrawRequest request = new WithdrawRequest("1111111111", new BigDecimal("2000.00"), "ATM withdrawal");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(2L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.withdraw("alice", request);

        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("3000.00"), sourceAccount.getBalance());

        verify(bankAccountRepository).save(sourceAccount);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal amount exceeds balance")
    void testWithdraw_InsufficientBalance() {
        WithdrawRequest request = new WithdrawRequest("1111111111", new BigDecimal("10000.00"), null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.withdraw("alice", request));
    }

    @Test
    @DisplayName("Should successfully transfer funds between accounts")
    void testTransfer_Success() {
        TransferRequest request = new TransferRequest("1111111111", "2222222222", new BigDecimal("1500.00"), "Rent payment");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumberWithLock("2222222222")).thenReturn(Optional.of(targetAccount));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(3L);
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.transfer("alice", request);

        assertNotNull(response);
        assertEquals(TransactionType.TRANSFER, response.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals("1111111111", response.getSourceAccountNumber());
        assertEquals("2222222222", response.getTargetAccountNumber());

        assertEquals(new BigDecimal("3500.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("2500.00"), targetAccount.getBalance());

        verify(bankAccountRepository).save(sourceAccount);
        verify(bankAccountRepository).save(targetAccount);
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException on self-transfer attempt")
    void testTransfer_SelfTransfer() {
        TransferRequest request = new TransferRequest("1111111111", "1111111111", new BigDecimal("500.00"), null);

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer("alice", request));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when transfer exceeds source balance")
    void testTransfer_InsufficientBalance() {
        TransferRequest request = new TransferRequest("1111111111", "2222222222", new BigDecimal("6000.00"), null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumberWithLock("2222222222")).thenReturn(Optional.of(targetAccount));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.transfer("alice", request));
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when target account is blocked")
    void testTransfer_TargetInactive() {
        targetAccount.setStatus(AccountStatus.BLOCKED);
        TransferRequest request = new TransferRequest("1111111111", "2222222222", new BigDecimal("500.00"), null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumberWithLock("2222222222")).thenReturn(Optional.of(targetAccount));

        assertThrows(AccountInactiveException.class, () ->
                transactionService.transfer("alice", request));
    }
}
