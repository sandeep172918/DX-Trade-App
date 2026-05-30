package com.dx.trade.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;

    @Column(nullable = false)
    private String stockSymbol;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "executed_at", updatable = false)
    private LocalDateTime executedAt;
}
