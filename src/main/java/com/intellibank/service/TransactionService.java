package com.intellibank.service;

import com.intellibank.dto.DepositRequest;
import com.intellibank.dto.TransactionResponse;
import com.intellibank.dto.TransferRequest;
import com.intellibank.dto.WithdrawRequest;
import com.intellibank.entity.*;
import com.intellibank.exception.AccountInactiveException;
import com.intellibank.exception.AccountNotFoundException;
import com.intellibank.exception.InsufficientBalanceException;
import com.intellibank.exception.InvalidTransactionException;
import com.intellibank.repository.BankAccountRepository;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.TransactionRepository;
import com.intellibank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TransactionService(BankAccountRepository bankAccountRepository,
                              TransactionRepository transactionRepository,
                              CustomerRepository customerRepository,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionResponse deposit(String username, DepositRequest request) {
        Customer customer = getCustomerByUsername(username);

        BankAccount account = bankAccountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to deposit into this account.");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot deposit: Account is " + account.getStatus());
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        bankAccountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setTargetAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Deposit into account " + account.getAccountNumber());

        Transaction savedTxn = transactionRepository.save(transaction);

        auditService.log(username, AuditAction.DEPOSIT,
                "Deposited ₹" + request.getAmount() + " into account " + account.getAccountNumber());

        return TransactionResponse.fromEntity(savedTxn);
    }

    @Transactional
    public TransactionResponse withdraw(String username, WithdrawRequest request) {
        Customer customer = getCustomerByUsername(username);

        BankAccount account = bankAccountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to withdraw from this account.");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot withdraw: Account is " + account.getStatus());
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: ₹" + account.getBalance()
                    + ", Requested: ₹" + request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setSourceAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Withdrawal from account " + account.getAccountNumber());

        Transaction savedTxn = transactionRepository.save(transaction);

        auditService.log(username, AuditAction.WITHDRAWAL,
                "Withdrew ₹" + request.getAmount() + " from account " + account.getAccountNumber());

        return TransactionResponse.fromEntity(savedTxn);
    }

    @Transactional
    public TransactionResponse transfer(String username, TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new InvalidTransactionException("Source and target account numbers cannot be the same.");
        }

        Customer customer = getCustomerByUsername(username);

        // Deadlock Prevention: Deterministic Lock Ordering
        String accA = request.getFromAccountNumber();
        String accB = request.getToAccountNumber();

        BankAccount firstLocked;
        BankAccount secondLocked;

        if (accA.compareTo(accB) < 0) {
            firstLocked = bankAccountRepository.findByAccountNumberWithLock(accA)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + accA));
            secondLocked = bankAccountRepository.findByAccountNumberWithLock(accB)
                    .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + accB));
        } else {
            firstLocked = bankAccountRepository.findByAccountNumberWithLock(accB)
                    .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + accB));
            secondLocked = bankAccountRepository.findByAccountNumberWithLock(accA)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + accA));
        }

        BankAccount source = accA.equals(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;
        BankAccount target = accB.equals(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;

        if (!source.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to transfer from this account.");
        }

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot transfer: Source account is " + source.getStatus());
        }

        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot transfer: Target account is " + target.getStatus());
        }

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account. Available: ₹"
                    + source.getBalance() + ", Requested: ₹" + request.getAmount());
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        target.setBalance(target.getBalance().add(request.getAmount()));

        bankAccountRepository.save(source);
        bankAccountRepository.save(target);

        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setSourceAccount(source);
        transaction.setTargetAccount(target);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Transfer from " + source.getAccountNumber() + " to " + target.getAccountNumber());

        Transaction savedTxn = transactionRepository.save(transaction);

        auditService.log(username, AuditAction.TRANSFER,
                "Transferred ₹" + request.getAmount() + " from " + source.getAccountNumber() + " to " + target.getAccountNumber());

        return TransactionResponse.fromEntity(savedTxn);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String username, String accountNumber, Pageable pageable) {
        Customer customer = getCustomerByUsername(username);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to view transactions for this account.");
        }

        Page<Transaction> transactions = transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                account.getId(), account.getId(), pageable);

        return transactions.map(TransactionResponse::fromEntity);
    }

    private Customer getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Customer profile not found for user: " + username));
    }
}
