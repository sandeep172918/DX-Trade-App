package com.dx.trade.backend.controller;

import com.dx.trade.backend.model.Transaction;
import com.dx.trade.backend.model.TransactionType;
import com.dx.trade.backend.model.Wallet;
import com.dx.trade.backend.model.User;
import com.dx.trade.backend.repository.UserRepository;
import com.dx.trade.backend.security.UserDetailsImpl;
import com.dx.trade.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${dx.trade.securePaymentKey:secure-payment-secret-1234}")
    private String securePaymentKey;

    @GetMapping
    public ResponseEntity<Wallet> getMyWallet() {
        UserDetailsImpl user = currentUser();
        return ResponseEntity.ok(walletService.getWalletByUserId(user.getId()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getMyTransactions() {
        UserDetailsImpl user = currentUser();
        return ResponseEntity.ok(walletService.getTransactionHistory(user.getId()));
    }

    /**
     * Simulate a deposit directly (for development/demo — bypasses Stripe).
     * Requires valid secure payment API key in header X-Secure-Payment-Key.
     * Requires valid 4-digit user MPIN in request body.
     * POST /api/wallet/simulate-deposit  { "amount": 1000, "mpin": "0000" }
     */
    @PostMapping("/simulate-deposit")
    public ResponseEntity<?> simulateDeposit(
            @RequestHeader(value = "X-Secure-Payment-Key", required = false) String clientKey,
            @RequestBody Map<String, Object> body) {
        
        if (securePaymentKey == null || !securePaymentKey.equals(clientKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Forbidden: Invalid secure payment authorization key"
            ));
        }

        UserDetailsImpl userDetails = currentUser();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate MPIN
        String providedMpin = body.getOrDefault("mpin", "").toString();
        if (user.getMpin() == null || !user.getMpin().equals(providedMpin)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Unauthorized: Invalid 4-digit MPIN"
            ));
        }

        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        // Security Cap: limit transaction size
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(new BigDecimal("5000.00")) > 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Deposit limit violation: Amount must be between $0.01 and $5,000.00 per transaction"
            ));
        }

        // Security Cap: limit maximum total balance
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        BigDecimal newBalance = wallet.getBalance().add(amount);
        if (newBalance.compareTo(new BigDecimal("100000.00")) > 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Deposit limit violation: Maximum simulated balance is $100,000.00"
            ));
        }

        String ref = "TX-DEP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        walletService.addFunds(user.getId(), amount, ref);
        
        Wallet updatedWallet = walletService.getWalletByUserId(user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Funds added successfully",
                "newBalance", updatedWallet.getBalance(),
                "transactionReference", ref
        ));
    }

    /**
     * Change or Set User MPIN.
     * POST /api/wallet/change-mpin { "password": "...", "oldMpin": "...", "newMpin": "..." }
     */
    @PostMapping("/change-mpin")
    public ResponseEntity<?> changeMpin(@RequestBody Map<String, Object> body) {
        UserDetailsImpl userDetails = currentUser();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String password = body.getOrDefault("password", "").toString();
        String oldMpin = body.getOrDefault("oldMpin", "").toString();
        String newMpin = body.getOrDefault("newMpin", "").toString();

        // 1. Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid password verification"
            ));
        }

        // 2. Verify old MPIN
        if (!user.getMpin().equals(oldMpin)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Incorrect previous MPIN"
            ));
        }

        // 3. Validate new MPIN format (must be 4 digits)
        if (!newMpin.matches("\\d{4}")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "New MPIN must be exactly 4 numeric digits"
            ));
        }

        // 4. Update
        user.setMpin(newMpin);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "MPIN updated successfully"));
    }

    private UserDetailsImpl currentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
