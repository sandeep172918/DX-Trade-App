package com.dx.trade.backend.repository;

import com.dx.trade.backend.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findByIsActiveTrue();
}
