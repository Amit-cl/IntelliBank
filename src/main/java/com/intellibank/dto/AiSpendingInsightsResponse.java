package com.intellibank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiSpendingInsightsResponse {

    private String accountNumber;
    private String accountType;
    private BigDecimal currentBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalSpent;
    private BigDecimal netCashFlow;
    private String topSpendingCategory;
    private String aiSummaryAndAdvice;
    private List<String> recentSecurityEvents;
}
