package com.dx.trade.backend.service;

import com.dx.trade.backend.model.*;
import com.dx.trade.backend.repository.TransactionRepository;
import com.dx.trade.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }

    @Transactional
    public void addFunds(Long userId, BigDecimal amount, String transactionReference) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .transactionReference(transactionReference)
                .build();
        transactionRepository.save(transaction);
    }

    @Transactional
    public void deductFunds(Long userId, BigDecimal amount, TransactionType type) {
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        String ref = "TX-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .transactionReference(ref)
                .build();
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId);
    }
}
