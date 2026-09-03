package com.intellibank.repository;

import com.intellibank.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            Long sourceAccountId, Long targetAccountId, Pageable pageable);

    Optional<Transaction> findByTransactionReference(String transactionReference);
}
