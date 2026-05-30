package com.dx.trade.backend.matching;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import com.dx.trade.backend.model.Order;

import lombok.Getter;

@Getter
public class OrderBook {
    private final String stockSymbol;
    private final PriorityBlockingQueue<Order> buyOrders;
    private final PriorityBlockingQueue<Order> sellOrders;

    public OrderBook(String stockSymbol) {
        this.stockSymbol = stockSymbol;

       
        this.buyOrders = new PriorityBlockingQueue<>(100,    // buy orders: highest price first, then earliest time
                Comparator.comparing(Order::getPrice).reversed()
                        .thenComparing(Order::getCreatedAt));


        this.sellOrders = new PriorityBlockingQueue<>(100,      // sell orders: lowest price first, then earliest time
                Comparator.comparing(Order::getPrice)
                        .thenComparing(Order::getCreatedAt));
    }

    public void addOrder(Order order) {
        if (order.getSide().name().equals("BUY")) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }
    }
}
