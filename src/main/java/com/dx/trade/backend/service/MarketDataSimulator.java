package com.dx.trade.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dx.trade.backend.model.Stock;
import com.dx.trade.backend.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MarketDataSimulator {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataSimulator.class);
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
            
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 1000)  //at every 1 seconds data get updated
    public void simulatePrices() {
        List<Stock> activeStocks = stockRepository.findByIsActiveTrue();
        if (activeStocks.isEmpty()) {
            return;
        }

        String symbols = activeStocks.stream()
                .map(Stock::getSymbol)
                .collect(Collectors.joining(","));

        try {
            String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + symbols;  //api from yahoo finnace
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.warn("Failed to fetch real market data. Status code: {}", response.statusCode());
                return;
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode resultNode = rootNode.path("quoteResponse").path("result");

            if (resultNode.isArray()) {
                for (JsonNode stockNode : resultNode) {
                    String symbol = stockNode.path("symbol").asText();
                    double priceVal = stockNode.path("regularMarketPrice").asDouble();
                    BigDecimal newPrice = BigDecimal.valueOf(priceVal).setScale(2, RoundingMode.HALF_UP);

                    stockRepository.findById(symbol).ifPresent(stock -> {
                        stock.setCurrentPrice(newPrice);
                        stockRepository.save(stock);
                        
                        // Broadcast update to WebSocket topic
                        messagingTemplate.convertAndSend("/topic/stocks", stock);
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching real market data: {}", e.getMessage());
        }
    }
}
