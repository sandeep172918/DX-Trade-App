package com.dx.trade.backend.repository;

import com.dx.trade.backend.model.Order;
import com.dx.trade.backend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStockSymbolAndStatusIn(String stockSymbol, List<OrderStatus> statuses);
}
