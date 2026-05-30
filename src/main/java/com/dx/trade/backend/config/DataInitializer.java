package com.dx.trade.backend.config;

import com.dx.trade.backend.model.Role;
import com.dx.trade.backend.model.Stock;
import com.dx.trade.backend.model.User;
import com.dx.trade.backend.model.Wallet;
import com.dx.trade.backend.repository.StockRepository;
import com.dx.trade.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (stockRepository.count() == 0) {
            stockRepository.saveAll(Arrays.asList(
                    Stock.builder().symbol("AAPL").name("Apple Inc.").currentPrice(new BigDecimal("185.20")).isActive(true).build(),
                    Stock.builder().symbol("MSFT").name("Microsoft Corp.").currentPrice(new BigDecimal("410.50")).isActive(true).build(),
                    Stock.builder().symbol("GOOGL").name("Alphabet Inc.").currentPrice(new BigDecimal("150.10")).isActive(true).build(),
                    Stock.builder().symbol("AMZN").name("Amazon.com Inc.").currentPrice(new BigDecimal("178.40")).isActive(true).build(),
                    Stock.builder().symbol("TSLA").name("Tesla Inc.").currentPrice(new BigDecimal("175.60")).isActive(true).build(),
                    Stock.builder().symbol("NVDA").name("NVIDIA Corp.").currentPrice(new BigDecimal("880.00")).isActive(true).build(),
                    Stock.builder().symbol("META").name("Meta Platforms Inc.").currentPrice(new BigDecimal("495.30")).isActive(true).build()
            ));
        }


    }
}
