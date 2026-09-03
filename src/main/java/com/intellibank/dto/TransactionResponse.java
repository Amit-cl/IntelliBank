package com.intellibank.dto;

import com.intellibank.entity.Transaction;
import com.intellibank.entity.TransactionStatus;
import com.intellibank.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String transactionReference;
    private TransactionType transactionType;
    private BigDecimal amount;
    private TransactionStatus status;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private String description;
    private LocalDateTime timestamp;

    public static TransactionResponse fromEntity(Transaction transaction) {
        String sourceAcc = transaction.getSourceAccount() != null
                ? transaction.getSourceAccount().getAccountNumber()
                : null;
        String targetAcc = transaction.getTargetAccount() != null
                ? transaction.getTargetAccount().getAccountNumber()
                : null;

        return new TransactionResponse(
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getStatus(),
                sourceAcc,
                targetAcc,
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
