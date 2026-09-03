package com.intellibank.controller;

import com.intellibank.dto.DepositRequest;
import com.intellibank.dto.TransactionResponse;
import com.intellibank.dto.TransferRequest;
import com.intellibank.dto.WithdrawRequest;
import com.intellibank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private String getAuthenticatedUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        throw new AccessDeniedException("Unauthenticated user");
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> deposit(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request) {
        String username = getAuthenticatedUsername(authentication);
        TransactionResponse response = transactionService.deposit(username, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {
        String username = getAuthenticatedUsername(authentication);
        TransactionResponse response = transactionService.withdraw(username, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request) {
        String username = getAuthenticatedUsername(authentication);
        TransactionResponse response = transactionService.transfer(username, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            Authentication authentication,
            @PathVariable String accountNumber,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = getAuthenticatedUsername(authentication);
        Page<TransactionResponse> history = transactionService.getTransactionHistory(username, accountNumber, pageable);
        return ResponseEntity.ok(history);
    }
}
