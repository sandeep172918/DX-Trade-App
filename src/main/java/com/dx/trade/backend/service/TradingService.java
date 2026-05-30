package com.dx.trade.backend.service;

import com.dx.trade.backend.matching.MatchingEngine;
import com.dx.trade.backend.model.*;
import com.dx.trade.backend.repository.OrderRepository;
import com.dx.trade.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TradingService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private WalletService walletService;

    @Transactional
    public Order placeOrder(Long userId, String symbol, OrderType type, OrderSide side,
                            BigDecimal quantity, BigDecimal price) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate BUY orders: check sufficient balance
        if (side == OrderSide.BUY && type == OrderType.LIMIT) {
            BigDecimal totalCost = price.multiply(quantity);
            if (user.getWallet().getBalance().compareTo(totalCost) < 0) {
                throw new RuntimeException("Insufficient balance. Required: " + totalCost
                        + ", Available: " + user.getWallet().getBalance());
            }
        }

        Order order = Order.builder()
                .user(user)
                .stockSymbol(symbol)
                .type(type)
                .side(side)
                .quantity(quantity)
                .filledQuantity(BigDecimal.ZERO)
                .price(price)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        matchingEngine.processOrder(savedOrder);
        return savedOrder;
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: you do not own this order");
        }
        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel a " + order.getStatus().name().toLowerCase() + " order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Remove from in-memory order book
        matchingEngine.removeOrder(order);
    }
}
