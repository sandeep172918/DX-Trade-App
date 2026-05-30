package com.dx.trade.backend.repository;

import com.dx.trade.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Transaction> findByTransactionReference(String transactionReference);
}
