package com.dx.trade.backend.repository;

import com.dx.trade.backend.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUserId(Long userId);
    Optional<Holding> findByUserIdAndStockSymbol(Long userId, String stockSymbol);
}
