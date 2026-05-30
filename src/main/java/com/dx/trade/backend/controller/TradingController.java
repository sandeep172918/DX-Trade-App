package com.dx.trade.backend.controller;

import com.dx.trade.backend.dto.OrderRequest;
import com.dx.trade.backend.model.Order;
import com.dx.trade.backend.security.UserDetailsImpl;
import com.dx.trade.backend.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading")
public class TradingController {
    @Autowired
    private TradingService tradingService;

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Order order = tradingService.placeOrder(
                userDetails.getId(),
                request.getSymbol(),
                request.getType(),
                request.getSide(),
                request.getQuantity(),
                request.getPrice()
        );
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getMyOrders() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(tradingService.getUserOrders(userDetails.getId()));
    }

    @PostMapping("/order/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        tradingService.cancelOrder(id, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
