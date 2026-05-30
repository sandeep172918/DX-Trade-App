package com.dx.trade.backend.controller;

import com.dx.trade.backend.model.Holding;
import com.dx.trade.backend.model.Stock;
import com.dx.trade.backend.repository.HoldingRepository;
import com.dx.trade.backend.repository.StockRepository;
import com.dx.trade.backend.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/holdings")
    public ResponseEntity<List<Map<String, Object>>> getMyHoldings() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        List<Holding> holdings = holdingRepository.findByUserId(userDetails.getId());

        List<Map<String, Object>> result = holdings.stream().map(holding -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", holding.getId());
            dto.put("stockSymbol", holding.getStockSymbol());
            dto.put("quantity", holding.getQuantity());
            dto.put("averageBuyPrice", holding.getAverageBuyPrice());

            Optional<Stock> stockOpt = stockRepository.findById(holding.getStockSymbol());
            BigDecimal currentPrice = stockOpt.map(Stock::getCurrentPrice).orElse(holding.getAverageBuyPrice());
            dto.put("currentPrice", currentPrice);

            BigDecimal invested = holding.getQuantity().multiply(holding.getAverageBuyPrice());
            BigDecimal currentValue = holding.getQuantity().multiply(currentPrice);
            BigDecimal unrealizedPnl = currentValue.subtract(invested);
            BigDecimal unrealizedPnlPercent = invested.compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedPnl.divide(invested, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            dto.put("currentValue", currentValue);
            dto.put("unrealizedPnl", unrealizedPnl);
            dto.put("unrealizedPnlPercent", unrealizedPnlPercent.setScale(2, RoundingMode.HALF_UP));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
