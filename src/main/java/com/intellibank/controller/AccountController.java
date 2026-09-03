package com.intellibank.controller;

import com.intellibank.dto.AccountResponse;
import com.intellibank.dto.CreateAccountRequest;
import com.intellibank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
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

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountResponse> createAccount(
            Authentication authentication,
            @Valid @RequestBody CreateAccountRequest request) {
        String username = getAuthenticatedUsername(authentication);
        AccountResponse response = accountService.createAccount(username, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        String username = getAuthenticatedUsername(authentication);
        List<AccountResponse> accounts = accountService.getMyAccounts(username);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            Authentication authentication,
            @PathVariable String accountNumber) {
        String username = getAuthenticatedUsername(authentication);
        AccountResponse account = accountService.getAccountByNumber(username, accountNumber);
        return ResponseEntity.ok(account);
    }
}
