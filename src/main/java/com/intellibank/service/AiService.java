package com.intellibank.service;

import com.intellibank.dto.AiChatRequest;
import com.intellibank.dto.AiChatResponse;
import com.intellibank.dto.AiSpendingInsightsResponse;
import com.intellibank.entity.*;
import com.intellibank.exception.AccountNotFoundException;
import com.intellibank.repository.AuditLogRepository;
import com.intellibank.repository.BankAccountRepository;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.TransactionRepository;
import com.intellibank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GroqApiClient groqApiClient;

    public AiService(BankAccountRepository bankAccountRepository,
                     TransactionRepository transactionRepository,
                     AuditLogRepository auditLogRepository,
                     UserRepository userRepository,
                     CustomerRepository customerRepository,
                     GroqApiClient groqApiClient) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.groqApiClient = groqApiClient;
    }

    @Transactional(readOnly = true)
    public AiSpendingInsightsResponse generateSpendingInsights(String username, String accountNumber) {
        Customer customer = getCustomerByUsername(username);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have access to this account.");
        }

        // Fetch recent 30 transactions
        Page<Transaction> txnPage = transactionRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                        account.getId(), account.getId(), PageRequest.of(0, 30));
        List<Transaction> transactions = txnPage.getContent();

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        Map<String, BigDecimal> categorySpending = new HashMap<>();

        for (Transaction txn : transactions) {
            boolean isIncoming = txn.getTargetAccount() != null && txn.getTargetAccount().getId().equals(account.getId());
            boolean isOutgoing = txn.getSourceAccount() != null && txn.getSourceAccount().getId().equals(account.getId());

            if (txn.getTransactionType() == TransactionType.DEPOSIT || (txn.getTransactionType() == TransactionType.TRANSFER && isIncoming)) {
                totalDeposits = totalDeposits.add(txn.getAmount());
            } else if (txn.getTransactionType() == TransactionType.WITHDRAWAL || (txn.getTransactionType() == TransactionType.TRANSFER && isOutgoing)) {
                totalSpent = totalSpent.add(txn.getAmount());
                String category = categorizeDescription(txn.getDescription());
                categorySpending.put(category, categorySpending.getOrDefault(category, BigDecimal.ZERO).add(txn.getAmount()));
            }
        }

        String topCategory = categorySpending.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General Expenses");

        BigDecimal netCashFlow = totalDeposits.subtract(totalSpent);

        // Fetch recent 5 audit logs
        List<AuditLog> auditLogs = auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(username, PageRequest.of(0, 5))
                .getContent();

        List<String> securityEvents = auditLogs.stream()
                .map(log -> "[" + log.getAction() + "] " + log.getDetails())
                .collect(Collectors.toList());

        // Build RAG prompt for Groq
        String systemPrompt = "You are IntelliBank's AI Financial Advisor and Account Security Analyst.\n" +
                "Provide an intelligent, encouraging, and concise financial analysis in 3-4 bullet points.\n" +
                "Highlight: 1) Monthly spending & cash flow, 2) Actionable savings tip, 3) Account health & security assurance.\n" +
                "Format amounts in Indian Rupees (₹).";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Customer: ").append(customer.getFirstName()).append(" ").append(customer.getLastName()).append("\n");
        userPrompt.append("Account Number: ").append(account.getAccountNumber()).append(" (").append(account.getAccountType()).append(")\n");
        userPrompt.append("Current Balance: ₹").append(account.getBalance()).append("\n");
        userPrompt.append("Recent Total Inflow (Deposits): ₹").append(totalDeposits).append("\n");
        userPrompt.append("Recent Total Outflow (Spent): ₹").append(totalSpent).append("\n");
        userPrompt.append("Net Cash Flow: ₹").append(netCashFlow).append("\n");
        userPrompt.append("Top Expense Category: ").append(topCategory).append("\n");
        userPrompt.append("Recent Security Events: ").append(securityEvents).append("\n");
        userPrompt.append("Please provide a smart financial summary and personalized advice.");

        String aiAdvice = groqApiClient.generateCompletion(systemPrompt, userPrompt.toString());

        return new AiSpendingInsightsResponse(
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                totalDeposits,
                totalSpent,
                netCashFlow,
                topCategory,
                aiAdvice,
                securityEvents
        );
    }

    @Transactional(readOnly = true)
    public AiChatResponse chatWithAssistant(String username, AiChatRequest request) {
        Customer customer = getCustomerByUsername(username);
        List<BankAccount> accounts = bankAccountRepository.findByCustomerId(customer.getId());

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Customer Name: ").append(customer.getFirstName()).append(" ").append(customer.getLastName()).append("\n");
        contextBuilder.append("Accounts:\n");

        for (BankAccount acc : accounts) {
            contextBuilder.append("- Account ").append(acc.getAccountNumber())
                    .append(" (").append(acc.getAccountType()).append("): Balance ₹")
                    .append(acc.getBalance()).append(", Status: ").append(acc.getStatus()).append("\n");

            // Add last 5 transactions for each account
            Page<Transaction> recentTxns = transactionRepository
                    .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(acc.getId(), acc.getId(), PageRequest.of(0, 5));
            for (Transaction t : recentTxns.getContent()) {
                contextBuilder.append("  * Txn: ").append(t.getTransactionType())
                        .append(" ₹").append(t.getAmount())
                        .append(" (").append(t.getStatus()).append(")")
                        .append(" - ").append(t.getDescription() != null ? t.getDescription() : "No desc")
                        .append("\n");
            }
        }

        // Add recent audit logs
        List<AuditLog> auditLogs = auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(username, PageRequest.of(0, 5))
                .getContent();
        contextBuilder.append("Recent Security Logs:\n");
        for (AuditLog log : auditLogs) {
            contextBuilder.append("- ").append(log.getAction()).append(": ").append(log.getDetails()).append("\n");
        }

        String systemPrompt = "You are IntelliBank's AI Personal Banking Assistant and Financial Advisor.\n" +
                "You have access to the verified, real-time banking data for customer " + customer.getFirstName() + ".\n" +
                "Rules:\n" +
                "1. Answer questions about account balance, transactions, spending, and security accurately using the verified context.\n" +
                "2. If the user asks whether they can afford a purchase, evaluate their current balance and financial cushion responsibly.\n" +
                "3. If they ask a general banking concept question, explain it clearly.\n" +
                "4. Always use Indian Rupees (₹) for currency.\n" +
                "5. Keep responses concise, friendly, and helpful.\n\n" +
                "Verified Financial Context:\n" + contextBuilder.toString();

        String reply = groqApiClient.generateCompletion(systemPrompt, request.getPrompt());

        return new AiChatResponse(reply, groqApiClient.getModel(), LocalDateTime.now());
    }

    private Customer getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Customer profile not found for user: " + username));
    }

    private String categorizeDescription(String desc) {
        if (desc == null) return "Miscellaneous";
        String lower = desc.toLowerCase();
        if (lower.contains("food") || lower.contains("swiggy") || lower.contains("zomato") || lower.contains("restaurant")) return "Dining & Food";
        if (lower.contains("amazon") || lower.contains("flipkart") || lower.contains("shopping") || lower.contains("store")) return "Shopping";
        if (lower.contains("rent") || lower.contains("house") || lower.contains("landlord")) return "Rent & Housing";
        if (lower.contains("bill") || lower.contains("electricity") || lower.contains("water") || lower.contains("recharge")) return "Utilities & Bills";
        if (lower.contains("uber") || lower.contains("ola") || lower.contains("metro") || lower.contains("petrol") || lower.contains("fuel")) return "Travel & Fuel";
        if (lower.contains("transfer")) return "Peer-to-Peer Transfer";
        return "General Expense";
    }
}
