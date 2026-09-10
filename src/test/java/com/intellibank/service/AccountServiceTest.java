package com.intellibank.service;

import com.intellibank.dto.AccountResponse;
import com.intellibank.dto.CreateAccountRequest;
import com.intellibank.entity.AccountStatus;
import com.intellibank.entity.AccountType;
import com.intellibank.entity.BankAccount;
import com.intellibank.entity.Customer;
import com.intellibank.entity.User;
import com.intellibank.exception.AccountNotFoundException;
import com.intellibank.repository.BankAccountRepository;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");

        testCustomer = new Customer();
        testCustomer.setId(10L);
        testCustomer.setUser(testUser);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
    }

    @Test
    @DisplayName("Should create account with specified initial balance")
    void testCreateAccount_WithInitialBalance() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("5000.00"));

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        BankAccount savedAccount = new BankAccount();
        savedAccount.setId(100L);
        savedAccount.setAccountNumber("1234567890");
        savedAccount.setCustomer(testCustomer);
        savedAccount.setAccountType(AccountType.SAVINGS);
        savedAccount.setBalance(new BigDecimal("5000.00"));
        savedAccount.setStatus(AccountStatus.ACTIVE);
        savedAccount.setCreatedAt(LocalDateTime.now());

        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount("john_doe", request);

        assertNotNull(response);
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals(AccountType.SAVINGS, response.getAccountType());
        assertEquals(new BigDecimal("5000.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Should default initial balance to 0.00 when null")
    void testCreateAccount_NullInitialBalance_DefaultsToZero() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CURRENT, null);

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        BankAccount savedAccount = new BankAccount();
        savedAccount.setId(101L);
        savedAccount.setAccountNumber("9876543210");
        savedAccount.setCustomer(testCustomer);
        savedAccount.setAccountType(AccountType.CURRENT);
        savedAccount.setBalance(BigDecimal.ZERO);
        savedAccount.setStatus(AccountStatus.ACTIVE);
        savedAccount.setCreatedAt(LocalDateTime.now());

        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount("john_doe", request);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals(AccountType.CURRENT, response.getAccountType());
    }

    @Test
    @DisplayName("Should return all accounts belonging to customer")
    void testGetMyAccounts_Success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));

        BankAccount account1 = new BankAccount();
        account1.setAccountNumber("1111111111");
        account1.setAccountType(AccountType.SAVINGS);
        account1.setBalance(new BigDecimal("1000.00"));
        account1.setStatus(AccountStatus.ACTIVE);

        BankAccount account2 = new BankAccount();
        account2.setAccountNumber("2222222222");
        account2.setAccountType(AccountType.CURRENT);
        account2.setBalance(new BigDecimal("2000.00"));
        account2.setStatus(AccountStatus.ACTIVE);

        when(bankAccountRepository.findByCustomerId(10L)).thenReturn(List.of(account1, account2));

        List<AccountResponse> results = accountService.getMyAccounts("john_doe");

        assertEquals(2, results.size());
        assertEquals("1111111111", results.get(0).getAccountNumber());
        assertEquals("2222222222", results.get(1).getAccountNumber());
    }

    @Test
    @DisplayName("Should return account by number when customer is owner")
    void testGetAccountByNumber_Success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));

        BankAccount account = new BankAccount();
        account.setAccountNumber("1111111111");
        account.setCustomer(testCustomer);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);

        when(bankAccountRepository.findByAccountNumber("1111111111")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByNumber("john_doe", "1111111111");

        assertNotNull(response);
        assertEquals("1111111111", response.getAccountNumber());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException if account does not exist")
    void testGetAccountByNumber_NotFound() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));
        when(bankAccountRepository.findByAccountNumber("9999999999")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                accountService.getAccountByNumber("john_doe", "9999999999"));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException if account belongs to another customer")
    void testGetAccountByNumber_UnauthorizedUser() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));

        Customer otherCustomer = new Customer();
        otherCustomer.setId(99L);

        BankAccount otherAccount = new BankAccount();
        otherAccount.setAccountNumber("8888888888");
        otherAccount.setCustomer(otherCustomer);

        when(bankAccountRepository.findByAccountNumber("8888888888")).thenReturn(Optional.of(otherAccount));

        assertThrows(AccessDeniedException.class, () ->
                accountService.getAccountByNumber("john_doe", "8888888888"));
    }
}
