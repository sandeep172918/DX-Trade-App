package com.dx.trade.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private boolean isActive;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
