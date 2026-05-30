package com.dx.trade.backend.controller;

import com.dx.trade.backend.model.Stock;
import com.dx.trade.backend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/public/stocks")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    private final Random random = new Random();

    @GetMapping
    public ResponseEntity<List<Stock>> getAllActiveStocks() {
        return ResponseEntity.ok(stockRepository.findByIsActiveTrue());
    }

    /**
     * Generates synthetic candle data.
     */
    @GetMapping("/{symbol}/candles")
    public ResponseEntity<List<Map<String, Object>>> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "60") String resolution,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        long now = Instant.now().getEpochSecond();
        long fromTime = (from != null) ? from : now - (24 * 60 * 60);
        long toTime = (to != null) ? to : now;

        long interval;
        try {
            interval = switch (resolution) {
                case "1" -> 60L;
                case "5" -> 300L;
                case "15" -> 900L;
                case "30" -> 1800L;
                case "60" -> 3600L;
                case "D" -> 86400L;
                default -> 3600L;
            };
        } catch (Exception e) {
            interval = 3600L;
        }

        List<Map<String, Object>> candles = new ArrayList<>();
        Stock stock = stockRepository.findById(symbol).orElse(null);
        double basePrice = (stock != null) ? stock.getCurrentPrice().doubleValue() : 100.0;

        for (long t = fromTime; t < toTime; t += interval) {
            double open = basePrice + (random.nextDouble() * 2 - 1);
            double high = open + random.nextDouble();
            double low = open - random.nextDouble();
            double close = low + (high - low) * random.nextDouble();
            long volume = 1000 + random.nextInt(9000);

            candles.add(Map.of(
                    "time", t,
                    "open", open,
                    "high", high,
                    "low", low,
                    "close", close,
                    "volume", volume
            ));
            basePrice = close;
        }

        return ResponseEntity.ok(candles);
    }

    /**
     * Returns a synthetic quote for a symbol based on current price.
     */
    @GetMapping("/{symbol}/quote")
    public ResponseEntity<Map<String, Object>> getQuote(@PathVariable String symbol) {
        Stock stock = stockRepository.findById(symbol).orElse(null);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }

        double current = stock.getCurrentPrice().doubleValue();
        double change = (random.nextDouble() * 2 - 1);
        double prevClose = current - change;
        double changePercent = (change / prevClose) * 100;

        return ResponseEntity.ok(Map.of(
                "c", current,
                "h", current + random.nextDouble(),
                "l", current - random.nextDouble(),
                "o", current + (random.nextDouble() * 0.5 - 0.25),
                "pc", prevClose,
                "d", change,
                "dp", changePercent
        ));
    }
}
