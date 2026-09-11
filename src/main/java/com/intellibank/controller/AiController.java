package com.intellibank.controller;

import com.intellibank.dto.AiChatRequest;
import com.intellibank.dto.AiChatResponse;
import com.intellibank.dto.AiSpendingInsightsResponse;
import com.intellibank.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
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

    @PostMapping("/chat")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AiChatResponse> chat(
            Authentication authentication,
            @Valid @RequestBody AiChatRequest request) {
        String username = getAuthenticatedUsername(authentication);
        AiChatResponse response = aiService.chatWithAssistant(username, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AiSpendingInsightsResponse> getInsights(
            Authentication authentication,
            @PathVariable String accountNumber) {
        String username = getAuthenticatedUsername(authentication);
        AiSpendingInsightsResponse response = aiService.generateSpendingInsights(username, accountNumber);
        return ResponseEntity.ok(response);
    }
}
