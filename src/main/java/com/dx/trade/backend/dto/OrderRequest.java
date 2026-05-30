package com.dx.trade.backend.dto;

import com.dx.trade.backend.model.OrderSide;
import com.dx.trade.backend.model.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderRequest {
    private String symbol;
    private OrderType type;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal price;
}
