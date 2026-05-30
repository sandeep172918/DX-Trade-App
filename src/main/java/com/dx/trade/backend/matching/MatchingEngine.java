package com.dx.trade.backend.matching;

import com.dx.trade.backend.model.*;
import com.dx.trade.backend.repository.HoldingRepository;
import com.dx.trade.backend.repository.OrderRepository;
import com.dx.trade.backend.repository.StockRepository;
import com.dx.trade.backend.repository.TradeRepository;
import com.dx.trade.backend.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MatchingEngine {

    private static final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private HoldingRepository holdingRepository;

    public void processOrder(Order order) {
        OrderBook book = orderBooks.computeIfAbsent(order.getStockSymbol(), OrderBook::new);
        if (order.getSide() == OrderSide.BUY) {
            matchBuyOrder(order, book);
        } else {
            matchSellOrder(order, book);
        }
    }

    public void removeOrder(Order order) {
        OrderBook book = orderBooks.get(order.getStockSymbol());
        if (book == null) return;
        if (order.getSide() == OrderSide.BUY) {
            book.getBuyOrders().removeIf(o -> o.getId().equals(order.getId()));
        } else {
            book.getSellOrders().removeIf(o -> o.getId().equals(order.getId()));
        }
    }

    private void matchBuyOrder(Order buyOrder, OrderBook book) {
        while (buyOrder.getFilledQuantity().compareTo(buyOrder.getQuantity()) < 0) {
            Order bestSell = book.getSellOrders().peek();
            if (bestSell == null ||
                    (buyOrder.getType() == OrderType.LIMIT &&
                            buyOrder.getPrice().compareTo(bestSell.getPrice()) < 0)) {
                book.addOrder(buyOrder);
                break;
            }
            bestSell = book.getSellOrders().poll();
            executeTrade(buyOrder, bestSell);
        }
    }

    private void matchSellOrder(Order sellOrder, OrderBook book) {
        while (sellOrder.getFilledQuantity().compareTo(sellOrder.getQuantity()) < 0) {
            Order bestBuy = book.getBuyOrders().peek();
            if (bestBuy == null ||
                    (sellOrder.getType() == OrderType.LIMIT &&
                            sellOrder.getPrice().compareTo(bestBuy.getPrice()) > 0)) {
                book.addOrder(sellOrder);
                break;
            }
            bestBuy = book.getBuyOrders().poll();
            executeTrade(bestBuy, sellOrder);
        }
    }

    @Transactional
    protected void executeTrade(Order buyOrder, Order sellOrder) {
        BigDecimal tradeQuantity = buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity())
                .min(sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity()));

        BigDecimal tradePrice = sellOrder.getPrice(); // Price of the resting (sitting) order
        BigDecimal tradeValue = tradeQuantity.multiply(tradePrice);

        // --- Update fill amounts ---
        buyOrder.setFilledQuantity(buyOrder.getFilledQuantity().add(tradeQuantity));
        sellOrder.setFilledQuantity(sellOrder.getFilledQuantity().add(tradeQuantity));

        buyOrder.setStatus(buyOrder.getFilledQuantity().compareTo(buyOrder.getQuantity()) == 0
                ? OrderStatus.FILLED : OrderStatus.PARTIAL);
        sellOrder.setStatus(sellOrder.getFilledQuantity().compareTo(sellOrder.getQuantity()) == 0
                ? OrderStatus.FILLED : OrderStatus.PARTIAL);

        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        // --- Create Trade record ---
        Trade trade = Trade.builder()
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .stockSymbol(buyOrder.getStockSymbol())
                .quantity(tradeQuantity)
                .price(tradePrice)
                .build();
        tradeRepository.save(trade);

        // --- Update stock price ---
        stockRepository.findById(buyOrder.getStockSymbol()).ifPresent(stock -> {
            stock.setCurrentPrice(tradePrice);
            stockRepository.save(stock);
        });

        // --- Settle wallets ---
        try {
            walletService.deductFunds(buyOrder.getUser().getId(), tradeValue, TransactionType.TRADE_BUY);
        } catch (RuntimeException e) {
            logger.error("Failed to deduct funds from buyer {}: {}", buyOrder.getUser().getId(), e.getMessage());
        }
        walletService.addFunds(sellOrder.getUser().getId(), tradeValue, null);

        // --- Settle holdings ---
        updateBuyerHolding(buyOrder.getUser().getId(), buyOrder.getStockSymbol(), tradeQuantity, tradePrice);
        updateSellerHolding(sellOrder.getUser().getId(), sellOrder.getStockSymbol(), tradeQuantity);
    }

    private void updateBuyerHolding(Long userId, String symbol, BigDecimal quantity, BigDecimal price) {
        Holding holding = holdingRepository.findByUserIdAndStockSymbol(userId, symbol)
                .orElse(null);
        if (holding == null) {
            // Find the user reference from an existing holding or create fresh
            orderRepository.findByUserId(userId).stream().findFirst().ifPresent(order -> {
                Holding newHolding = Holding.builder()
                        .user(order.getUser())
                        .stockSymbol(symbol)
                        .quantity(quantity)
                        .averageBuyPrice(price)
                        .build();
                holdingRepository.save(newHolding);
            });
        } else {
            // Weighted average price: (existingQty * existingAvg + newQty * newPrice) / totalQty
            BigDecimal totalQty = holding.getQuantity().add(quantity);
            BigDecimal newAvg = holding.getQuantity().multiply(holding.getAverageBuyPrice())
                    .add(quantity.multiply(price))
                    .divide(totalQty, 8, RoundingMode.HALF_UP);
            holding.setQuantity(totalQty);
            holding.setAverageBuyPrice(newAvg);
            holdingRepository.save(holding);
        }
    }

    private void updateSellerHolding(Long userId, String symbol, BigDecimal quantity) {
        holdingRepository.findByUserIdAndStockSymbol(userId, symbol).ifPresent(holding -> {
            BigDecimal newQty = holding.getQuantity().subtract(quantity);
            if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                holdingRepository.delete(holding);
            } else {
                holding.setQuantity(newQty);
                holdingRepository.save(holding);
            }
        });
    }
}
