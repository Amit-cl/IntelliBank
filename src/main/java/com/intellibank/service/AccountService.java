package com.intellibank.service;

import com.intellibank.dto.AccountResponse;
import com.intellibank.dto.CreateAccountRequest;
import com.intellibank.entity.AccountStatus;
import com.intellibank.entity.BankAccount;
import com.intellibank.entity.Customer;
import com.intellibank.entity.User;
import com.intellibank.exception.AccountNotFoundException;
import com.intellibank.repository.BankAccountRepository;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(BankAccountRepository bankAccountRepository,
                          CustomerRepository customerRepository,
                          UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse createAccount(String username, CreateAccountRequest request) {
        Customer customer = getCustomerByUsername(username);

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal balance = request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO;

        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setAccountType(request.getAccountType());
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);

        BankAccount savedAccount = bankAccountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String username) {
        Customer customer = getCustomerByUsername(username);
        List<BankAccount> accounts = bankAccountRepository.findByCustomerId(customer.getId());
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String username, String accountNumber) {
        Customer customer = getCustomerByUsername(username);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with account number: " + accountNumber));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to access this account.");
        }

        return AccountResponse.fromEntity(account);
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with account number: " + accountNumber));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new com.intellibank.exception.InvalidTransactionException("Cannot update status of a CLOSED account.");
        }

        account.setStatus(newStatus);
        BankAccount updatedAccount = bankAccountRepository.save(account);
        return AccountResponse.fromEntity(updatedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetailsForAdmin(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with account number: " + accountNumber));
        return AccountResponse.fromEntity(account);
    }

    private Customer getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Customer profile not found for user: " + username));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 1000000000L + (long) (secureRandom.nextDouble() * 9000000000L);
            accountNumber = String.valueOf(number);
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
